import { FormEvent, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api, formatDateTime, statusLabel } from "../api/client";
import type { Me, MfaConfirmResult, MfaEnrollment, MfaFactor } from "../api/types";

export function ProfilePage({ me }: { me: Me }) {
  const profile = me.profile;
  const [factors, setFactors] = useState<MfaFactor[]>([]);
  const [enrollment, setEnrollment] = useState<MfaEnrollment | null>(null);
  const [recoveryCodes, setRecoveryCodes] = useState<string[] | null>(null);
  const [code, setCode] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  async function loadFactors() {
    setFactors(await api<MfaFactor[]>("/api/v1/me/mfa/factors"));
  }

  useEffect(() => {
    void loadFactors().catch((err) => setError((err as Error).message));
  }, []);

  async function startEnrollment() {
    setError(null);
    setSuccess(null);
    setRecoveryCodes(null);
    try {
      setEnrollment(await api<MfaEnrollment>("/api/v1/me/mfa/totp/enrollments", {
        method: "POST",
        body: JSON.stringify({ label: "Authenticator" }),
      }));
    } catch (err) {
      setError((err as Error).message);
    }
  }

  async function confirmEnrollment(event: FormEvent) {
    event.preventDefault();
    if (!enrollment) {
      return;
    }
    setError(null);
    try {
      const result = await api<MfaConfirmResult>(`/api/v1/me/mfa/totp/enrollments/${enrollment.id}/confirm`, {
        method: "POST",
        body: JSON.stringify({ code }),
      });
      setRecoveryCodes(result.recoveryCodes);
      setEnrollment(null);
      setCode("");
      setSuccess("已启用双因素认证。请立即保存恢复码，关闭后将无法再次查看。");
      await loadFactors();
    } catch (err) {
      setError((err as Error).message);
    }
  }

  async function disableFactor(id: string) {
    setError(null);
    try {
      await api(`/api/v1/me/mfa/factors/${id}/disable`, { method: "POST" });
      setSuccess("已停用该验证方式。");
      await loadFactors();
    } catch (err) {
      setError((err as Error).message);
    }
  }

  const activeFactors = factors.filter((item) => !item.disabledAt && item.verifiedAt);

  return (
    <div className="split">
      <section className="panel" style={{ width: "auto" }}>
        <h1>个人资料</h1>
        <p className="lede">这些信息来自身份主数据，如需更正请联系管理员。</p>
        <dl className="stack">
          <div>
            <div className="hint">用户名</div>
            <div>{profile.username}</div>
          </div>
          <div>
            <div className="hint">显示名</div>
            <div>{profile.displayName}</div>
          </div>
          <div>
            <div className="hint">邮箱</div>
            <div>{profile.email || "—"}</div>
          </div>
          <div>
            <div className="hint">手机</div>
            <div>{profile.mobile || "—"}</div>
          </div>
          <div>
            <div className="hint">状态</div>
            <div>{statusLabel(profile.status)}</div>
          </div>
          <div>
            <div className="hint">最近登录</div>
            <div>{formatDateTime(profile.lastLoginAt)}</div>
          </div>
        </dl>
      </section>
      <aside className="stack">
        <div className="panel" style={{ width: "auto" }}>
          <h2>安全</h2>
          <p>定期更新口令，并检查仍保持登录的设备。</p>
          <div className="btn-row">
            <Link className="btn" to="/password">
              修改密码
            </Link>
            <Link className="btn secondary" to="/sessions">
              管理会话
            </Link>
          </div>
        </div>
        <div className="panel" style={{ width: "auto" }}>
          <h2>双因素认证</h2>
          <p>启用 TOTP 后，登录时需要再输入验证码或一次性恢复码。</p>
          {error ? <div className="alert error">{error}</div> : null}
          {success ? <div className="alert ok">{success}</div> : null}
          {activeFactors.length === 0 && !enrollment ? (
            <button type="button" className="btn" onClick={() => void startEnrollment()}>
              启用身份验证器
            </button>
          ) : null}
          {enrollment ? (
            <form onSubmit={(event) => void confirmEnrollment(event)}>
              <p>请在身份验证器中扫描或手动添加以下密钥，然后输入当前验证码。</p>
              <div className="hint">otpauth</div>
              <p className="mono" style={{ wordBreak: "break-all" }}>
                {enrollment.otpauthUri}
              </p>
              <div className="hint">密钥（仅此次显示）</div>
              <p className="mono">{enrollment.secret}</p>
              <label className="field" htmlFor="totp-code">
                验证码
                <input
                  id="totp-code"
                  value={code}
                  onChange={(event) => setCode(event.target.value)}
                  autoComplete="one-time-code"
                  required
                />
              </label>
              <button className="btn" type="submit">
                确认启用
              </button>
            </form>
          ) : null}
          {recoveryCodes ? (
            <div>
              <div className="hint">恢复码</div>
              <ul className="mono">
                {recoveryCodes.map((item) => (
                  <li key={item}>{item}</li>
                ))}
              </ul>
            </div>
          ) : null}
          {factors.length > 0 ? (
            <ul className="stack">
              {factors.map((factor) => (
                <li key={factor.id}>
                  {factor.label || "Authenticator"} · {factor.verifiedAt ? "已验证" : "未验证"}
                  {factor.disabledAt ? " · 已停用" : null}
                  {factor.verifiedAt && !factor.disabledAt ? (
                    <button type="button" className="btn secondary" onClick={() => void disableFactor(factor.id)}>
                      停用
                    </button>
                  ) : null}
                </li>
              ))}
            </ul>
          ) : null}
        </div>
      </aside>
    </div>
  );
}

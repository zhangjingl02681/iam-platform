import { FormEvent, useEffect, useState } from "react";
import { api, formatDateTime, statusLabel } from "../../api/client";
import type { PamAccount, PamRequestItem, PamTarget } from "../../api/types";

export function AdminPam() {
  const [targets, setTargets] = useState<PamTarget[]>([]);
  const [accounts, setAccounts] = useState<PamAccount[]>([]);
  const [requests, setRequests] = useState<PamRequestItem[]>([]);
  const [selectedTarget, setSelectedTarget] = useState("");
  const [targetForm, setTargetForm] = useState({ type: "host", name: "", address: "", env: "prod" });
  const [accountForm, setAccountForm] = useState({ username: "", secret: "" });
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  async function load() {
    const nextTargets = await api<PamTarget[]>("/api/v1/pam/targets");
    setTargets(nextTargets);
    setRequests(await api<PamRequestItem[]>("/api/v1/pam/requests"));
    const targetId = selectedTarget || nextTargets[0]?.id || "";
    if (targetId) {
      setSelectedTarget(targetId);
      setAccounts(await api<PamAccount[]>(`/api/v1/pam/targets/${targetId}/accounts`));
    }
  }

  useEffect(() => {
    void load().catch((err) => setError((err as Error).message));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function createTarget(event: FormEvent) {
    event.preventDefault();
    setError(null);
    try {
      await api("/api/v1/pam/targets", { method: "POST", body: JSON.stringify(targetForm) });
      setSuccess("目标已登记。");
      setTargetForm({ type: "host", name: "", address: "", env: "prod" });
      await load();
    } catch (err) {
      setError((err as Error).message);
    }
  }

  async function createAccount(event: FormEvent) {
    event.preventDefault();
    setError(null);
    try {
      await api(`/api/v1/pam/targets/${selectedTarget}/accounts`, {
        method: "POST",
        body: JSON.stringify(accountForm),
      });
      setSuccess("账号已登记，密钥已写入本地保险库，不会出现在业务库中。");
      setAccountForm({ username: "", secret: "" });
      await load();
    } catch (err) {
      setError((err as Error).message);
    }
  }

  async function decide(id: string, action: "approve" | "deny") {
    setError(null);
    try {
      await api(`/api/v1/pam/requests/${id}/${action}`, { method: "POST", body: action === "deny" ? JSON.stringify({ reason: "已拒绝" }) : undefined });
      setSuccess(action === "approve" ? "申请已批准。" : "申请已拒绝。");
      await load();
    } catch (err) {
      setError((err as Error).message);
    }
  }

  return (
    <div className="split">
      <section>
        <h1>特权访问管理</h1>
        <p className="lede">登记目标和账号，审批员工的凭据签出申请。账号密钥只保存保险库引用。</p>
        {error ? <div className="alert error">{error}</div> : null}
        {success ? <div className="alert ok">{success}</div> : null}
        <h2>待处理申请</h2>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>原因</th>
                <th>状态</th>
                <th>窗口</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {requests.map((item) => (
                <tr key={item.id}>
                  <td>{item.reason}</td>
                  <td>{statusLabel(item.status)}</td>
                  <td>
                    {formatDateTime(item.startAt)} — {formatDateTime(item.endAt)}
                  </td>
                  <td>
                    {item.status === "PENDING" ? (
                      <div className="btn-row">
                        <button type="button" className="btn" onClick={() => void decide(item.id, "approve")}>
                          批准
                        </button>
                        <button type="button" className="btn secondary" onClick={() => void decide(item.id, "deny")}>
                          拒绝
                        </button>
                      </div>
                    ) : null}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <h2>当前目标账号</h2>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>用户名</th>
                <th>状态</th>
                <th>保险库引用</th>
              </tr>
            </thead>
            <tbody>
              {accounts.map((account) => (
                <tr key={account.id}>
                  <td className="mono">{account.username}</td>
                  <td>{statusLabel(account.status)}</td>
                  <td className="mono">{account.vaultRef || "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <h2>已登记目标</h2>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>名称</th>
                <th>类型</th>
                <th>地址</th>
              </tr>
            </thead>
            <tbody>
              {targets.map((target) => (
                <tr key={target.id}>
                  <td>
                    <button type="button" className="btn ghost" onClick={() => setSelectedTarget(target.id)}>
                      {target.name}
                    </button>
                  </td>
                  <td>{target.type}</td>
                  <td className="mono">{target.address}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
      <div className="stack">
        <form className="panel" style={{ width: "auto" }} onSubmit={(event) => void createTarget(event)}>
          <h2>登记目标</h2>
          <label className="field">
            类型
            <input required value={targetForm.type} onChange={(event) => setTargetForm({ ...targetForm, type: event.target.value })} />
          </label>
          <label className="field">
            名称
            <input required value={targetForm.name} onChange={(event) => setTargetForm({ ...targetForm, name: event.target.value })} />
          </label>
          <label className="field">
            地址
            <input value={targetForm.address} onChange={(event) => setTargetForm({ ...targetForm, address: event.target.value })} />
          </label>
          <label className="field">
            环境
            <input value={targetForm.env} onChange={(event) => setTargetForm({ ...targetForm, env: event.target.value })} />
          </label>
          <button className="btn" type="submit">
            保存目标
          </button>
        </form>
        <form className="panel" style={{ width: "auto" }} onSubmit={(event) => void createAccount(event)}>
          <h2>登记账号</h2>
          <p className="hint">当前目标：{targets.find((item) => item.id === selectedTarget)?.name || "请先选择或创建目标"}</p>
          <label className="field">
            用户名
            <input required value={accountForm.username} onChange={(event) => setAccountForm({ ...accountForm, username: event.target.value })} />
          </label>
          <label className="field">
            密钥
            <input required type="password" value={accountForm.secret} onChange={(event) => setAccountForm({ ...accountForm, secret: event.target.value })} />
          </label>
          <button className="btn" type="submit" disabled={!selectedTarget}>
            保存账号
          </button>
        </form>
      </div>
    </div>
  );
}

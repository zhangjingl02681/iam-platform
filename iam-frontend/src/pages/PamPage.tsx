import { FormEvent, useEffect, useState } from "react";
import { api, formatDateTime, statusLabel } from "../api/client";
import type { PamAccount, PamCheckout, PamRequestItem, PamSecret, PamTarget } from "../api/types";

export function PamPage() {
  const [targets, setTargets] = useState<PamTarget[]>([]);
  const [accounts, setAccounts] = useState<PamAccount[]>([]);
  const [requests, setRequests] = useState<PamRequestItem[]>([]);
  const [checkouts, setCheckouts] = useState<PamCheckout[]>([]);
  const [targetId, setTargetId] = useState("");
  const [accountId, setAccountId] = useState("");
  const [reason, setReason] = useState("");
  const [startAt, setStartAt] = useState("");
  const [endAt, setEndAt] = useState("");
  const [secret, setSecret] = useState<PamSecret | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  async function load() {
    const nextTargets = await api<PamTarget[]>("/api/v1/pam/targets");
    setTargets(nextTargets);
    const nextRequests = await api<PamRequestItem[]>("/api/v1/pam/requests");
    setRequests(nextRequests);
    setCheckouts(await api<PamCheckout[]>("/api/v1/pam/checkouts"));
  }

  useEffect(() => {
    void load().catch((err) => setError((err as Error).message));
  }, []);

  useEffect(() => {
    if (!targetId) {
      setAccounts([]);
      return;
    }
    void api<PamAccount[]>(`/api/v1/pam/targets/${targetId}/accounts`)
      .then(setAccounts)
      .catch((err) => setError((err as Error).message));
  }, [targetId]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    try {
      await api("/api/v1/pam/requests", {
        method: "POST",
        body: JSON.stringify({
          accountId,
          reason,
          startAt: new Date(startAt).toISOString(),
          endAt: new Date(endAt).toISOString(),
        }),
      });
      setSuccess("申请已提交，等待管理员审批。");
      setReason("");
      await load();
    } catch (err) {
      setError((err as Error).message);
    }
  }

  async function checkout(id: string) {
    setError(null);
    try {
      const result = await api<PamSecret>(`/api/v1/pam/requests/${id}/checkout`, { method: "POST" });
      setSecret(result);
      setSuccess("凭据已签出，请立即复制。过期或签入后将不再显示。");
      await load();
    } catch (err) {
      setError((err as Error).message);
    }
  }

  async function checkIn(id: string) {
    setError(null);
    try {
      await api(`/api/v1/pam/checkouts/${id}/check-in`, { method: "POST" });
      setSuccess("已提前签入。");
      await load();
    } catch (err) {
      setError((err as Error).message);
    }
  }

  return (
    <div className="split">
      <section>
        <h1>特权访问</h1>
        <p className="lede">申请在指定时间窗口内签出目标账号凭据。凭据不会写入业务库，仅在签出时显示一次。</p>
        {error ? <div className="alert error">{error}</div> : null}
        {success ? <div className="alert ok">{success}</div> : null}
        {secret ? (
          <div className="panel" style={{ width: "auto", marginBottom: "1rem" }}>
            <h2>刚签出的凭据</h2>
            <p>账号：{secret.username}</p>
            <p className="mono">{secret.secret}</p>
            <p className="hint">有效至 {formatDateTime(secret.expiresAt)}</p>
          </div>
        ) : null}
        <h2>我的申请</h2>
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
                    {item.status === "APPROVED" ? (
                      <button type="button" className="btn" onClick={() => void checkout(item.id)}>
                        签出凭据
                      </button>
                    ) : null}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <h2>签出记录</h2>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>状态</th>
                <th>到期</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {checkouts.map((item) => (
                <tr key={item.id}>
                  <td>{statusLabel(item.status)}</td>
                  <td>{formatDateTime(item.expiresAt)}</td>
                  <td>
                    {item.status === "ACTIVE" ? (
                      <button type="button" className="btn secondary" onClick={() => void checkIn(item.id)}>
                        提前签入
                      </button>
                    ) : null}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
      <form className="panel" style={{ width: "auto" }} onSubmit={(event) => void submit(event)}>
        <h2>新建申请</h2>
        <label className="field">
          目标
          <select required value={targetId} onChange={(event) => setTargetId(event.target.value)}>
            <option value="">请选择</option>
            {targets.map((target) => (
              <option key={target.id} value={target.id}>
                {target.name}
              </option>
            ))}
          </select>
        </label>
        <label className="field">
          账号
          <select required value={accountId} onChange={(event) => setAccountId(event.target.value)}>
            <option value="">请选择</option>
            {accounts.map((account) => (
              <option key={account.id} value={account.id}>
                {account.username}
              </option>
            ))}
          </select>
        </label>
        <label className="field">
          原因
          <textarea required value={reason} onChange={(event) => setReason(event.target.value)} />
        </label>
        <label className="field">
          开始时间
          <input required type="datetime-local" value={startAt} onChange={(event) => setStartAt(event.target.value)} />
        </label>
        <label className="field">
          结束时间
          <input required type="datetime-local" value={endAt} onChange={(event) => setEndAt(event.target.value)} />
        </label>
        <button className="btn" type="submit">
          提交申请
        </button>
      </form>
    </div>
  );
}

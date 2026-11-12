import { FormEvent, useState } from "react";
import { api, formatDateTime, statusLabel } from "../../api/client";
import { ConfirmDialog } from "../../components/ConfirmDialog";
import type { PageResponse, Session } from "../../api/types";

export function AdminSessions() {
  const [username, setUsername] = useState("");
  const [page, setPage] = useState<PageResponse<Session> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [revokeId, setRevokeId] = useState<string | null>(null);

  async function search(event?: FormEvent) {
    event?.preventDefault();
    const params = new URLSearchParams({ size: "20" });
    if (username) params.set("username", username);
    try {
      setPage(await api<PageResponse<Session>>(`/api/v1/sessions?${params}`));
      setError(null);
    } catch (err) {
      setError((err as Error).message);
    }
  }

  async function revoke() {
    if (!revokeId) return;
    try {
      await api(`/api/v1/sessions/${revokeId}/revoke`, { method: "POST" });
      setRevokeId(null);
      await search();
    } catch (err) {
      setError((err as Error).message);
    }
  }

  return (
    <div>
      <h1>会话管理</h1>
      <p className="lede">按用户查找登录会话，并可注销其中任意一条。注销会吊销关联的刷新令牌。</p>
      {error ? <div className="alert error">{error}</div> : null}
      <form className="filters" onSubmit={(event) => void search(event)}>
        <label className="field">
          用户名
          <input value={username} onChange={(e) => setUsername(e.target.value)} placeholder="精确用户名" />
        </label>
        <button className="btn" type="submit">
          查询
        </button>
      </form>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>用户</th>
              <th>IP</th>
              <th>状态</th>
              <th>最近活动</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {page?.content.map((session) => (
              <tr key={session.id}>
                <td>{session.username || session.userId}</td>
                <td className="mono">{session.ip || "—"}</td>
                <td>{session.current ? "当前会话" : statusLabel(session.status)}</td>
                <td>{formatDateTime(session.lastSeenAt)}</td>
                <td>
                  {session.status === "ACTIVE" ? (
                    <button type="button" className="btn secondary" onClick={() => setRevokeId(session.id)}>
                      注销
                    </button>
                  ) : null}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {page && page.content.length === 0 ? <div className="empty" style={{ marginTop: 16 }}>没有匹配的会话。</div> : null}
      <ConfirmDialog
        open={Boolean(revokeId)}
        title="注销会话"
        message="确认注销该会话？用户需要重新登录。"
        confirmLabel="确认注销"
        onClose={() => setRevokeId(null)}
        onConfirm={() => void revoke()}
      />
    </div>
  );
}

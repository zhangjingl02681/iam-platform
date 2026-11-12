import { useState } from "react";
import { ConfirmDialog } from "../components/ConfirmDialog";
import { formatDateTime, statusLabel } from "../api/client";
import type { Session } from "../api/types";

interface Props {
  sessions: Session[];
  loading: boolean;
  error: string | null;
  onRevoke: (id: string) => Promise<void>;
  onRevokeOthers: () => Promise<void>;
}

export function SessionsPage({ sessions, loading, error, onRevoke, onRevokeOthers }: Props) {
  const [pending, setPending] = useState<{ type: "one" | "others"; id?: string } | null>(null);
  const [busy, setBusy] = useState(false);

  async function confirm() {
    if (!pending) {
      return;
    }
    setBusy(true);
    try {
      if (pending.type === "others") {
        await onRevokeOthers();
      } else if (pending.id) {
        await onRevoke(pending.id);
      }
      setPending(null);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div>
      <div className="page-head">
        <div>
          <h1>我的会话</h1>
          <p className="lede">查看已登录的设备。注销某个会话会同时吊销与其关联的刷新令牌。</p>
        </div>
        <button type="button" className="btn secondary" onClick={() => setPending({ type: "others" })}>
          注销其他会话
        </button>
      </div>
      {error ? (
        <div className="alert error" role="alert">
          {error}
        </div>
      ) : null}
      {loading ? <div className="loading">正在加载会话…</div> : null}
      {!loading && sessions.length === 0 ? <div className="empty">目前没有会话记录。</div> : null}
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>状态</th>
              <th>IP</th>
              <th>设备 / 浏览器</th>
              <th>最近活动</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {sessions.map((session) => (
              <tr key={session.id}>
                <td>
                  <span className={`badge ${session.current ? "ok" : session.status === "ACTIVE" ? "" : "warn"}`}>
                    {session.current ? "当前会话" : statusLabel(session.status)}
                  </span>
                </td>
                <td className="mono">{session.ip || "—"}</td>
                <td>{session.userAgent || "未知客户端"}</td>
                <td>{formatDateTime(session.lastSeenAt)}</td>
                <td>
                  {session.status === "ACTIVE" && !session.current ? (
                    <button
                      type="button"
                      className="btn secondary"
                      onClick={() => setPending({ type: "one", id: session.id })}
                    >
                      注销此会话
                    </button>
                  ) : session.current ? (
                    <button
                      type="button"
                      className="btn secondary"
                      onClick={() => setPending({ type: "one", id: session.id })}
                    >
                      注销并退出
                    </button>
                  ) : null}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <ConfirmDialog
        open={Boolean(pending)}
        title="确认注销会话"
        message={
          pending?.type === "others"
            ? "将注销除当前设备以外的全部会话，这些设备需要重新登录。"
            : pending?.id && sessions.find((item) => item.id === pending.id)?.current
              ? "这将结束当前登录并返回登录页。是否确认？"
              : "确认注销该会话？对应设备将被退出。"
        }
        confirmLabel="确认注销"
        onClose={() => !busy && setPending(null)}
        onConfirm={() => void confirm()}
      />
    </div>
  );
}

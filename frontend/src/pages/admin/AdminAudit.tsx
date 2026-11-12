import { FormEvent, useEffect, useState } from "react";
import { api, formatDateTime } from "../../api/client";
import type { AuditEvent, PageResponse } from "../../api/types";

export function AdminAudit() {
  const [page, setPage] = useState<PageResponse<AuditEvent> | null>(null);
  const [filters, setFilters] = useState({ action: "", eventType: "", objectId: "", actorId: "" });
  const [error, setError] = useState<string | null>(null);

  async function load(event?: FormEvent) {
    event?.preventDefault();
    const params = new URLSearchParams({ size: "30" });
    Object.entries(filters).forEach(([key, value]) => {
      if (value) params.set(key, value);
    });
    try {
      setPage(await api<PageResponse<AuditEvent>>(`/api/v1/audit-events?${params}`));
      setError(null);
    } catch (err) {
      setError((err as Error).message);
    }
  }

  useEffect(() => {
    void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div>
      <h1>审计事件</h1>
      <p className="lede">审计记录只可查询，不能修改或删除。</p>
      {error ? <div className="alert error">{error}</div> : null}
      <form className="filters" onSubmit={(event) => void load(event)}>
        <label className="field">
          动作
          <input value={filters.action} onChange={(e) => setFilters({ ...filters, action: e.target.value })} placeholder="create / update" />
        </label>
        <label className="field">
          事件类型
          <input value={filters.eventType} onChange={(e) => setFilters({ ...filters, eventType: e.target.value })} placeholder="user.create" />
        </label>
        <label className="field">
          对象 ID
          <input value={filters.objectId} onChange={(e) => setFilters({ ...filters, objectId: e.target.value })} />
        </label>
        <label className="field">
          操作者 ID
          <input value={filters.actorId} onChange={(e) => setFilters({ ...filters, actorId: e.target.value })} />
        </label>
        <button className="btn secondary" type="submit">
          筛选
        </button>
      </form>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>时间</th>
              <th>事件</th>
              <th>动作</th>
              <th>对象</th>
              <th>结果</th>
            </tr>
          </thead>
          <tbody>
            {page?.content.map((event) => (
              <tr key={event.id}>
                <td>{formatDateTime(event.occurredAt)}</td>
                <td className="mono">{event.eventType}</td>
                <td>{event.action}</td>
                <td className="mono">
                  {event.objectType || "—"}
                  {event.objectId ? ` / ${event.objectId}` : ""}
                </td>
                <td>{event.result}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

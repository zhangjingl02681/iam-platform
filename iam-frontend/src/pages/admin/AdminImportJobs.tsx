import { FormEvent, useEffect, useState } from "react";
import { api, formatDateTime, statusLabel } from "../../api/client";
import type { ImportJob, PageResponse } from "../../api/types";

export function AdminImportJobs() {
  const [page, setPage] = useState<PageResponse<ImportJob> | null>(null);
  const [type, setType] = useState("user_csv");
  const [file, setFile] = useState<File | null>(null);
  const [latest, setLatest] = useState<ImportJob | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function load() {
    setPage(await api<PageResponse<ImportJob>>("/api/v1/import-jobs?size=20"));
  }

  useEffect(() => {
    void load().catch((err) => setError((err as Error).message));
  }, []);

  async function create(event: FormEvent) {
    event.preventDefault();
    setError(null);
    if (!file) {
      setError("请选择要导入的 CSV 文件。");
      return;
    }
    try {
      const body = new FormData();
      body.append("type", type);
      body.append("file", file);
      const created = await api<ImportJob>("/api/v1/import-jobs", { method: "POST", body });
      setLatest(created);
      setFile(null);
      await load();
    } catch (err) {
      setError((err as Error).message);
    }
  }

  const jobs = page?.content ?? [];
  const highlight = latest ?? jobs[0] ?? null;

  return (
    <div>
      <h1>导入任务</h1>
      <p className="lede">
        上传 CSV 以创建或更新用户。列：username, display_name, email, org_code, status, temporary_password。账号不会被物理删除。
      </p>
      {error ? <div className="alert error">{error}</div> : null}
      <form className="filters" onSubmit={(event) => void create(event)}>
        <label className="field">
          类型
          <input value={type} onChange={(e) => setType(e.target.value)} required />
        </label>
        <label className="field">
          CSV 文件
          <input
            type="file"
            accept=".csv,text/csv"
            onChange={(e) => setFile(e.target.files?.[0] ?? null)}
          />
        </label>
        <button className="btn" type="submit">
          上传并导入
        </button>
      </form>
      {highlight ? (
        <div className="panel" style={{ width: "auto", marginBottom: 16 }}>
          <h2>最近一次导入</h2>
          <p>
            共 {highlight.total ?? 0} 行，成功 {highlight.successCount ?? 0}，失败 {highlight.failedCount ?? 0}。状态：
            {statusLabel(highlight.status)}
          </p>
          {highlight.errorReport ? <pre className="hint">{highlight.errorReport}</pre> : null}
        </div>
      ) : null}
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>类型</th>
              <th>文件</th>
              <th>状态</th>
              <th>成功 / 失败</th>
              <th>完成时间</th>
            </tr>
          </thead>
          <tbody>
            {jobs.map((job) => (
              <tr key={job.id}>
                <td>{job.type}</td>
                <td className="mono">{job.fileRef || "—"}</td>
                <td>{statusLabel(job.status)}</td>
                <td>
                  {job.successCount ?? 0} / {job.failedCount ?? 0}
                </td>
                <td>{formatDateTime(job.finishedAt || job.createdAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {page && page.content.length === 0 ? <div className="empty" style={{ marginTop: 16 }}>还没有导入任务记录。</div> : null}
    </div>
  );
}

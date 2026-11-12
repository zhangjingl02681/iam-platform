import { FormEvent, useEffect, useState } from "react";
import { api, formatDateTime, statusLabel } from "../../api/client";
import type { ImportJob, PageResponse } from "../../api/types";

export function AdminImportJobs() {
  const [page, setPage] = useState<PageResponse<ImportJob> | null>(null);
  const [type, setType] = useState("user_csv");
  const [fileRef, setFileRef] = useState("");
  const [error, setError] = useState<string | null>(null);

  async function load() {
    setPage(await api<PageResponse<ImportJob>>("/api/v1/import-jobs?size=20"));
  }

  useEffect(() => {
    void load().catch((err) => setError((err as Error).message));
  }, []);

  async function create(event: FormEvent) {
    event.preventDefault();
    try {
      await api("/api/v1/import-jobs", {
        method: "POST",
        body: JSON.stringify({ type, fileRef }),
      });
      setFileRef("");
      await load();
    } catch (err) {
      setError((err as Error).message);
    }
  }

  return (
    <div>
      <h1>导入任务</h1>
      <p className="lede">当前仅记录导入任务，不解析上传文件。解析与执行将在后续迭代完成。</p>
      {error ? <div className="alert error">{error}</div> : null}
      <form className="filters" onSubmit={(event) => void create(event)}>
        <label className="field">
          类型
          <input value={type} onChange={(e) => setType(e.target.value)} required />
        </label>
        <label className="field">
          文件引用
          <input value={fileRef} onChange={(e) => setFileRef(e.target.value)} placeholder="file://imports/users.csv" />
        </label>
        <button className="btn" type="submit">
          登记任务
        </button>
      </form>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>类型</th>
              <th>文件</th>
              <th>状态</th>
              <th>创建时间</th>
            </tr>
          </thead>
          <tbody>
            {page?.content.map((job) => (
              <tr key={job.id}>
                <td>{job.type}</td>
                <td className="mono">{job.fileRef || "—"}</td>
                <td>{statusLabel(job.status)}</td>
                <td>{formatDateTime(job.createdAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {page && page.content.length === 0 ? <div className="empty" style={{ marginTop: 16 }}>还没有导入任务记录。</div> : null}
    </div>
  );
}

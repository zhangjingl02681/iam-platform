import { FormEvent, useEffect, useState } from "react";
import { api, statusLabel } from "../../api/client";
import type { Application, Entitlement, PageResponse } from "../../api/types";

const empty = {
  id: "",
  code: "",
  name: "",
  description: "",
  launchUrl: "",
  icon: "",
  accentColor: "#1f3a5f",
  status: "ACTIVE",
};

export function AdminApplications() {
  const [page, setPage] = useState<PageResponse<Application> | null>(null);
  const [form, setForm] = useState(empty);
  const [entitlements, setEntitlements] = useState<Entitlement[]>([]);
  const [grantType, setGrantType] = useState("USER");
  const [grantSubject, setGrantSubject] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  async function load() {
    setPage(await api<PageResponse<Application>>("/api/v1/applications?size=50"));
  }

  async function loadEntitlements(applicationId: string) {
    setEntitlements(await api<Entitlement[]>(`/api/v1/applications/${applicationId}/entitlements`));
  }

  useEffect(() => {
    void load().catch((err) => setError((err as Error).message));
  }, []);

  function edit(app: Application) {
    setForm({
      id: app.id,
      code: app.code,
      name: app.name,
      description: app.description || "",
      launchUrl: app.launchUrl || "",
      icon: app.icon || "",
      accentColor: app.accentColor || "#1f3a5f",
      status: app.status,
    });
    setGrantSubject("");
    setGrantType("USER");
    void loadEntitlements(app.id).catch((err) => setError((err as Error).message));
  }

  async function save(event: FormEvent) {
    event.preventDefault();
    setError(null);
    try {
      if (form.id) {
        await api(`/api/v1/applications/${form.id}`, {
          method: "PATCH",
          body: JSON.stringify({
            name: form.name,
            description: form.description,
            launchUrl: form.launchUrl,
            icon: form.icon,
            accentColor: form.accentColor,
            status: form.status,
          }),
        });
        setSuccess("应用已更新。");
      } else {
        await api("/api/v1/applications", {
          method: "POST",
          body: JSON.stringify({
            code: form.code,
            name: form.name,
            description: form.description,
            launchUrl: form.launchUrl,
            icon: form.icon,
            accentColor: form.accentColor,
            status: form.status,
          }),
        });
        setSuccess("应用已创建。");
        setForm(empty);
      }
      await load();
    } catch (err) {
      setError((err as Error).message);
    }
  }

  async function addGrant(event: FormEvent) {
    event.preventDefault();
    if (!form.id) return;
    setError(null);
    try {
      await api(`/api/v1/applications/${form.id}/entitlements`, {
        method: "POST",
        body: JSON.stringify({ subjectType: grantType, subjectId: grantSubject.trim() }),
      });
      setGrantSubject("");
      setSuccess("授权已添加。");
      await loadEntitlements(form.id);
    } catch (err) {
      setError((err as Error).message);
    }
  }

  async function removeGrant(id: string) {
    if (!form.id) return;
    try {
      await api(`/api/v1/applications/${form.id}/entitlements/${id}`, { method: "DELETE" });
      await loadEntitlements(form.id);
    } catch (err) {
      setError((err as Error).message);
    }
  }

  return (
    <div className="split">
      <section>
        <h1>应用目录</h1>
        <p className="lede">配置门户可打开的应用，并为受限应用分配用户、组织或角色授权。</p>
        {error ? <div className="alert error">{error}</div> : null}
        {success ? <div className="alert ok">{success}</div> : null}
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>名称</th>
                <th>编码</th>
                <th>启动地址</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              {page?.content.map((app) => (
                <tr key={app.id}>
                  <td>
                    <button type="button" className="btn ghost" onClick={() => edit(app)}>
                      {app.name}
                    </button>
                  </td>
                  <td className="mono">{app.code}</td>
                  <td className="mono">{app.launchUrl || "—"}</td>
                  <td>{statusLabel(app.status)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
      <div>
        <form className="panel" style={{ width: "auto" }} onSubmit={(event) => void save(event)}>
          <h2>{form.id ? "编辑应用" : "新建应用"}</h2>
          <label className="field">
            编码
            <input required disabled={Boolean(form.id)} value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} />
          </label>
          <label className="field">
            名称
            <input required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          </label>
          <label className="field">
            描述
            <textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
          </label>
          <label className="field">
            启动地址
            <input value={form.launchUrl} placeholder="https:// 或 /path" onChange={(e) => setForm({ ...form, launchUrl: e.target.value })} />
          </label>
          <label className="field">
            图标关键字
            <input value={form.icon} onChange={(e) => setForm({ ...form, icon: e.target.value })} />
          </label>
          <label className="field">
            强调色
            <input value={form.accentColor} onChange={(e) => setForm({ ...form, accentColor: e.target.value })} />
          </label>
          <label className="field">
            状态
            <select value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })}>
              <option value="ACTIVE">启用</option>
              <option value="DISABLED">停用</option>
            </select>
          </label>
          <div className="btn-row">
            <button className="btn" type="submit">
              保存
            </button>
            <button type="button" className="btn secondary" onClick={() => setForm(empty)}>
              新建
            </button>
          </div>
        </form>
        {form.id ? (
          <form className="panel" style={{ width: "auto", marginTop: 16 }} onSubmit={(event) => void addGrant(event)}>
            <h2>访问授权</h2>
            <p className="hint">没有授权记录时，所有已登录用户都可以在门户中看到该应用。一旦添加授权，仅匹配的用户、组织或角色可见。</p>
            {entitlements.length === 0 ? <p className="hint">当前没有授权，该应用对所有已登录用户开放。</p> : null}
            <ul>
              {entitlements.map((item) => (
                <li key={item.id}>
                  {item.subjectType} <span className="mono">{item.subjectId}</span>{" "}
                  <button type="button" className="btn ghost" onClick={() => void removeGrant(item.id)}>
                    移除
                  </button>
                </li>
              ))}
            </ul>
            <label className="field">
              主体类型
              <select value={grantType} onChange={(e) => setGrantType(e.target.value)}>
                <option value="USER">USER</option>
                <option value="ORG">ORG</option>
                <option value="ROLE">ROLE</option>
              </select>
            </label>
            <label className="field">
              主体 ID
              <input required value={grantSubject} onChange={(e) => setGrantSubject(e.target.value)} />
            </label>
            <button className="btn" type="submit">
              添加授权
            </button>
          </form>
        ) : null}
      </div>
    </div>
  );
}

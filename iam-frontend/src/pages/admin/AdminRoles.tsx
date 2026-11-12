import { FormEvent, useEffect, useState } from "react";
import { api, statusLabel } from "../../api/client";
import type { PageResponse, Role } from "../../api/types";

const empty = { id: "", code: "", name: "", description: "", status: "ACTIVE" };

export function AdminRoles() {
  const [page, setPage] = useState<PageResponse<Role> | null>(null);
  const [form, setForm] = useState(empty);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  async function load() {
    setPage(await api<PageResponse<Role>>("/api/v1/roles?size=50"));
  }

  useEffect(() => {
    void load().catch((err) => setError((err as Error).message));
  }, []);

  function edit(role: Role) {
    setForm({
      id: role.id,
      code: role.code,
      name: role.name,
      description: role.description || "",
      status: role.status,
    });
  }

  async function save(event: FormEvent) {
    event.preventDefault();
    setError(null);
    try {
      if (form.id) {
        await api(`/api/v1/roles/${form.id}`, {
          method: "PATCH",
          body: JSON.stringify({ name: form.name, description: form.description, status: form.status }),
        });
        setSuccess("角色已更新。");
      } else {
        await api("/api/v1/roles", {
          method: "POST",
          body: JSON.stringify({
            code: form.code,
            name: form.name,
            description: form.description,
            status: form.status,
          }),
        });
        setSuccess("角色已创建。");
        setForm(empty);
      }
      await load();
    } catch (err) {
      setError((err as Error).message);
    }
  }

  return (
    <div className="split">
      <section>
        <h1>角色</h1>
        <p className="lede">维护角色定义，并在用户详情中为账号分配角色。内置 admin 角色不能删除。</p>
        {error ? <div className="alert error">{error}</div> : null}
        {success ? <div className="alert ok">{success}</div> : null}
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>名称</th>
                <th>编码</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              {page?.content.map((role) => (
                <tr key={role.id}>
                  <td>
                    <button type="button" className="btn ghost" onClick={() => edit(role)}>
                      {role.name}
                    </button>
                  </td>
                  <td className="mono">{role.code}</td>
                  <td>{statusLabel(role.status)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
      <form className="panel" style={{ width: "auto" }} onSubmit={(event) => void save(event)}>
        <h2>{form.id ? "编辑角色" : "新建角色"}</h2>
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
    </div>
  );
}

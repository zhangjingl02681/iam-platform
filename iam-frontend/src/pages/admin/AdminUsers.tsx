import { FormEvent, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { api, formatDateTime, statusLabel } from "../../api/client";
import { ConfirmDialog } from "../../components/ConfirmDialog";
import type { OrgNode, PageResponse, Role, UserProfile } from "../../api/types";

export function AdminUsers() {
  const [username, setUsername] = useState("");
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);
  const [data, setData] = useState<PageResponse<UserProfile> | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function load(nextPage = page) {
    try {
      const params = new URLSearchParams({ page: String(nextPage), size: "20" });
      if (username) params.set("username", username);
      if (status) params.set("status", status);
      setData(await api<PageResponse<UserProfile>>(`/api/v1/users?${params}`));
      setError(null);
      setPage(nextPage);
    } catch (err) {
      setError((err as Error).message);
    }
  }

  useEffect(() => {
    void load(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div>
      <div className="page-head">
        <div>
          <h1>用户</h1>
          <p className="lede">账号只能停用或终止，不会被物理删除。</p>
        </div>
        <Link className="btn" to="/admin/users/new">
          新建用户
        </Link>
      </div>
      <form
        className="filters"
        onSubmit={(event) => {
          event.preventDefault();
          void load(0);
        }}
      >
        <label className="field">
          用户名
          <input value={username} onChange={(e) => setUsername(e.target.value)} placeholder="支持模糊查找" />
        </label>
        <label className="field">
          状态
          <select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">全部</option>
            <option value="ACTIVE">启用</option>
            <option value="DISABLED">停用</option>
            <option value="LOCKED">锁定</option>
            <option value="TERMINATED">已终止</option>
          </select>
        </label>
        <button className="btn secondary" type="submit">
          筛选
        </button>
      </form>
      {error ? <div className="alert error">{error}</div> : null}
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>用户名</th>
              <th>显示名</th>
              <th>邮箱</th>
              <th>状态</th>
              <th>更新时间</th>
            </tr>
          </thead>
          <tbody>
            {data?.content.map((user) => (
              <tr key={user.id}>
                <td>
                  <Link to={`/admin/users/${user.id}`}>{user.username}</Link>
                </td>
                <td>{user.displayName}</td>
                <td>{user.email || "—"}</td>
                <td>
                  <span className={`badge ${user.status === "ACTIVE" ? "ok" : user.status === "TERMINATED" ? "danger" : "warn"}`}>
                    {statusLabel(user.status)}
                  </span>
                </td>
                <td>{formatDateTime(user.updatedAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {data ? (
        <div className="pager">
          <span>
            共 {data.totalElements} 人，第 {data.page + 1}/{Math.max(data.totalPages, 1)} 页
          </span>
          <div className="btn-row">
            <button className="btn secondary" type="button" disabled={data.page === 0} onClick={() => void load(data.page - 1)}>
              上一页
            </button>
            <button
              className="btn secondary"
              type="button"
              disabled={data.page + 1 >= data.totalPages}
              onClick={() => void load(data.page + 1)}
            >
              下一页
            </button>
          </div>
        </div>
      ) : null}
    </div>
  );
}

function flattenOrgs(nodes: OrgNode[], acc: OrgNode[] = []): OrgNode[] {
  for (const node of nodes) {
    acc.push(node);
    if (node.children?.length) flattenOrgs(node.children, acc);
  }
  return acc;
}

export function AdminUserEditor() {
  const { id } = useParams();
  const isNew = !id || id === "new";
  const navigate = useNavigate();
  const [user, setUser] = useState<UserProfile | null>(null);
  const [orgs, setOrgs] = useState<OrgNode[]>([]);
  const [form, setForm] = useState({
    username: "",
    displayName: "",
    email: "",
    mobile: "",
    employeeNo: "",
    password: "",
    primaryOrgId: "",
  });
  const [resetPassword, setResetPassword] = useState("");
  const [roles, setRoles] = useState<Role[]>([]);
  const [assignRoleId, setAssignRoleId] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [confirm, setConfirm] = useState<"disable" | "enable" | "terminate" | null>(null);

  useEffect(() => {
    void api<OrgNode[]>("/api/v1/orgs/tree").then(setOrgs);
    void api<PageResponse<Role>>("/api/v1/roles?size=100").then((result) => setRoles(result.content));
    if (!isNew && id) {
      void api<UserProfile>(`/api/v1/users/${id}`).then((loaded) => {
        setUser(loaded);
        setForm({
          username: loaded.username,
          displayName: loaded.displayName,
          email: loaded.email || "",
          mobile: loaded.mobile || "",
          employeeNo: loaded.employeeNo || "",
          password: "",
          primaryOrgId: loaded.primaryOrgId || "",
        });
      });
    }
  }, [id, isNew]);

  function setField(name: keyof typeof form, value: string) {
    setForm((current) => ({ ...current, [name]: value }));
  }

  async function save(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setSuccess(null);
    try {
      if (isNew) {
        const created = await api<UserProfile>("/api/v1/users", {
          method: "POST",
          body: JSON.stringify({
            username: form.username,
            displayName: form.displayName,
            email: form.email || null,
            mobile: form.mobile || null,
            employeeNo: form.employeeNo || null,
            password: form.password || undefined,
            primaryOrgId: form.primaryOrgId || null,
          }),
        });
        setSuccess("用户已创建。");
        navigate(`/admin/users/${created.id}`, { replace: true });
        return;
      }
      const updated = await api<UserProfile>(`/api/v1/users/${id}`, {
        method: "PATCH",
        body: JSON.stringify({
          displayName: form.displayName,
          email: form.email || null,
          mobile: form.mobile || null,
          employeeNo: form.employeeNo || null,
        }),
      });
      if (form.primaryOrgId && form.primaryOrgId !== (user?.primaryOrgId || "")) {
        await api(`/api/v1/users/${id}/assignments`, {
          method: "POST",
          body: JSON.stringify({ orgId: form.primaryOrgId, primary: true }),
        });
      }
      setUser(updated);
      setSuccess("资料已保存。");
    } catch (err) {
      setError((err as Error).message);
    }
  }

  async function applyStatus(status: string) {
    if (!id || isNew) return;
    try {
      const updated = await api<UserProfile>(`/api/v1/users/${id}`, {
        method: "PATCH",
        body: JSON.stringify({ status }),
      });
      setUser(updated);
      setSuccess(`状态已更新为${statusLabel(status)}。`);
      setConfirm(null);
    } catch (err) {
      setError((err as Error).message);
    }
  }

  async function doReset() {
    if (!id || isNew) return;
    try {
      await api(`/api/v1/users/${id}/password`, {
        method: "POST",
        body: JSON.stringify({ newPassword: resetPassword, mustChange: true }),
      });
      setResetPassword("");
      setSuccess("已重置口令，用户下次登录必须修改。");
    } catch (err) {
      setError((err as Error).message);
    }
  }

  async function assignRole() {
    if (!id || isNew || !assignRoleId) return;
    try {
      const updated = await api<UserProfile>(`/api/v1/users/${id}/roles`, {
        method: "POST",
        body: JSON.stringify({ roleId: assignRoleId }),
      });
      setUser(updated);
      setAssignRoleId("");
      setSuccess("角色已分配。");
    } catch (err) {
      setError((err as Error).message);
    }
  }

  async function removeRole(roleId: string) {
    if (!id || isNew) return;
    try {
      await api(`/api/v1/users/${id}/roles/${roleId}`, { method: "DELETE" });
      const updated = await api<UserProfile>(`/api/v1/users/${id}`);
      setUser(updated);
      setSuccess("角色已移除。");
    } catch (err) {
      setError((err as Error).message);
    }
  }

  const orgOptions = flattenOrgs(orgs);

  return (
    <div className="split">
      <form className="panel" style={{ width: "auto" }} onSubmit={(event) => void save(event)}>
        <h1>{isNew ? "新建用户" : form.displayName || "编辑用户"}</h1>
        {error ? <div className="alert error">{error}</div> : null}
        {success ? <div className="alert ok">{success}</div> : null}
        <label className="field">
          用户名
          <input value={form.username} disabled={!isNew} required onChange={(e) => setField("username", e.target.value)} />
        </label>
        <label className="field">
          显示名
          <input value={form.displayName} required onChange={(e) => setField("displayName", e.target.value)} />
        </label>
        <label className="field">
          邮箱
          <input type="email" value={form.email} onChange={(e) => setField("email", e.target.value)} />
        </label>
        <label className="field">
          手机
          <input value={form.mobile} onChange={(e) => setField("mobile", e.target.value)} />
        </label>
        <label className="field">
          工号
          <input value={form.employeeNo} onChange={(e) => setField("employeeNo", e.target.value)} />
        </label>
        <label className="field">
          主组织
          <select value={form.primaryOrgId} onChange={(e) => setField("primaryOrgId", e.target.value)}>
            <option value="">未分配</option>
            {orgOptions.map((org) => (
              <option key={org.id} value={org.id}>
                {org.path || org.name}
              </option>
            ))}
          </select>
        </label>
        {isNew ? (
          <label className="field">
            初始密码
            <input type="password" value={form.password} required minLength={12} onChange={(e) => setField("password", e.target.value)} />
          </label>
        ) : null}
        <div className="btn-row">
          <button className="btn" type="submit">
            保存
          </button>
          <Link className="btn secondary" to="/admin/users">
            返回列表
          </Link>
        </div>
      </form>
      {!isNew && user ? (
        <aside className="stack">
          <div className="panel" style={{ width: "auto" }}>
            <h2>状态</h2>
            <p>
              当前为 <strong>{statusLabel(user.status)}</strong>，最近更新于 {formatDateTime(user.updatedAt)}。
            </p>
            <div className="btn-row">
              {user.status === "ACTIVE" ? (
                <button type="button" className="btn secondary" onClick={() => setConfirm("disable")}>
                  停用
                </button>
              ) : user.status === "DISABLED" || user.status === "LOCKED" ? (
                <button type="button" className="btn secondary" onClick={() => setConfirm("enable")}>
                  启用
                </button>
              ) : null}
              {user.status !== "TERMINATED" ? (
                <button type="button" className="btn danger" onClick={() => setConfirm("terminate")}>
                  终止账号
                </button>
              ) : null}
            </div>
          </div>
          <div className="panel" style={{ width: "auto" }}>
            <h2>角色</h2>
            <p className="hint">为该用户分配或移除角色。内置 admin 角色不能从最后一名管理员身上移除。</p>
            <ul>
              {(user.roles || []).map((role) => (
                <li key={role.id}>
                  {role.name} <span className="mono">{role.code}</span>{" "}
                  <button type="button" className="btn ghost" onClick={() => void removeRole(role.id)}>
                    移除
                  </button>
                </li>
              ))}
            </ul>
            {(user.roles || []).length === 0 ? <p className="hint">尚未分配角色。</p> : null}
            <label className="field">
              分配角色
              <select value={assignRoleId} onChange={(e) => setAssignRoleId(e.target.value)}>
                <option value="">选择角色</option>
                {roles.map((role) => (
                  <option key={role.id} value={role.id}>
                    {role.name} ({role.code})
                  </option>
                ))}
              </select>
            </label>
            <button type="button" className="btn secondary" disabled={!assignRoleId} onClick={() => void assignRole()}>
              分配
            </button>
          </div>
          <div className="panel" style={{ width: "auto" }}>
            <h2>重置口令</h2>
            <p className="hint">新密码不会出现在响应中。用户下次登录必须修改。</p>
            <label className="field">
              新密码
              <input type="password" value={resetPassword} minLength={12} onChange={(e) => setResetPassword(e.target.value)} />
            </label>
            <button type="button" className="btn secondary" disabled={resetPassword.length < 12} onClick={() => void doReset()}>
              重置密码
            </button>
          </div>
          <ConfirmDialog
            open={confirm !== null}
            title={confirm === "terminate" ? "终止账号" : confirm === "disable" ? "停用账号" : "启用账号"}
            message={
              confirm === "terminate"
                ? "终止后不可恢复登录，账号记录将保留。此操作不可逆。"
                : confirm === "disable"
                  ? "停用后该用户将无法登录，之后可以重新启用。"
                  : "确认重新启用该账号？"
            }
            confirmLabel={confirm === "terminate" ? "确认终止" : "确认"}
            danger={confirm === "terminate" || confirm === "disable"}
            onClose={() => setConfirm(null)}
            onConfirm={() =>
              void applyStatus(confirm === "terminate" ? "TERMINATED" : confirm === "disable" ? "DISABLED" : "ACTIVE")
            }
          />
        </aside>
      ) : null}
    </div>
  );
}

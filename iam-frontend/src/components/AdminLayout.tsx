import { NavLink, Outlet } from "react-router-dom";
import { ensureCsrfToken } from "../api/client";
import type { Me } from "../api/types";

export function AdminLayout({ me, children }: { me: Me; children?: React.ReactNode }) {
  async function logout() {
    const csrf = await ensureCsrfToken();
    const body = new URLSearchParams();
    if (csrf) body.set("_csrf", csrf);
    await fetch("/logout", {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/x-www-form-urlencoded", "X-CSRF-TOKEN": csrf, "X-XSRF-TOKEN": csrf },
      body,
    });
    window.location.href = "/login?logout";
  }

  return (
    <div className="admin-layout">
      <aside className="admin-side">
        <h2>管理控制台</h2>
        <div className="group">身份</div>
        <NavLink to="/admin/users">用户</NavLink>
        <NavLink to="/admin/orgs">组织</NavLink>
        <NavLink to="/admin/roles">角色</NavLink>
        <div className="group">应用与访问</div>
        <NavLink to="/admin/applications">应用目录</NavLink>
        <NavLink to="/admin/policies">访问策略</NavLink>
        <NavLink to="/admin/sessions">会话</NavLink>
        <NavLink to="/admin/pam">特权访问</NavLink>
        <NavLink to="/admin/signing-keys">签名密钥</NavLink>
        <div className="group">治理</div>
        <NavLink to="/admin/import-jobs">导入任务</NavLink>
        <NavLink to="/admin/audit">审计事件</NavLink>
        <div className="group">账户</div>
        <NavLink to="/">返回门户</NavLink>
        <p className="hint" style={{ color: "rgba(243,239,230,0.7)", padding: "12px 10px 0" }}>
          当前：{me.profile.displayName}
        </p>
        <button type="button" className="btn ghost" onClick={() => void logout()}>
          退出
        </button>
      </aside>
      <div className="admin-main">{children ?? <Outlet />}</div>
    </div>
  );
}

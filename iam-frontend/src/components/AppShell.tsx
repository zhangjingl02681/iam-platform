import { NavLink, Outlet } from "react-router-dom";
import { ensureCsrfToken } from "../api/client";
import type { Me } from "../api/types";

export function AppShell({ me, children }: { me: Me; children?: React.ReactNode }) {
  async function logout() {
    const csrf = await ensureCsrfToken();
    const body = new URLSearchParams();
    if (csrf) {
      body.set("_csrf", csrf);
    }
    await fetch("/logout", {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/x-www-form-urlencoded", "X-CSRF-TOKEN": csrf, "X-XSRF-TOKEN": csrf },
      body,
    });
    window.location.href = "/login?logout";
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <NavLink to="/" className="brand">
          <strong>企业内部 IAM</strong>
          <small>Portal</small>
        </NavLink>
        <nav className="nav" aria-label="主要导航">
          <NavLink to="/" end>
            应用门户
          </NavLink>
          <NavLink to="/profile">个人资料</NavLink>
          <NavLink to="/sessions">会话</NavLink>
          <NavLink to="/password">密码</NavLink>
          {me.admin ? <NavLink to="/admin">管理控制台</NavLink> : null}
          <button type="button" className="btn ghost" onClick={() => void logout()}>
            退出
          </button>
        </nav>
      </header>
      <main className="content">{children ?? <Outlet />}</main>
    </div>
  );
}

import { useCallback, useEffect, useState } from "react";
import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import { api } from "./api/client";
import type { Application, Me, Session } from "./api/types";
import { AdminLayout } from "./components/AdminLayout";
import { AppShell } from "./components/AppShell";
import { AdminApplications } from "./pages/admin/AdminApplications";
import { AdminAudit } from "./pages/admin/AdminAudit";
import { AdminHome } from "./pages/admin/AdminHome";
import { AdminImportJobs } from "./pages/admin/AdminImportJobs";
import { AdminOrgs } from "./pages/admin/AdminOrgs";
import { AdminPolicies } from "./pages/admin/AdminPolicies";
import { AdminRoles } from "./pages/admin/AdminRoles";
import { AdminSessions } from "./pages/admin/AdminSessions";
import { AdminUserEditor, AdminUsers } from "./pages/admin/AdminUsers";
import { ConsentPage } from "./pages/ConsentPage";
import { DirectoryPage, HandbookPage } from "./pages/ExamplesPages";
import { LoginPage } from "./pages/LoginPage";
import { PasswordPage } from "./pages/PasswordPage";
import { PortalPage } from "./pages/PortalPage";
import { ProfilePage } from "./pages/ProfilePage";
import { SessionsPage } from "./pages/SessionsPage";

export function App() {
  const location = useLocation();
  const publicPage = location.pathname === "/login" || location.pathname === "/consent";
  const [me, setMe] = useState<Me | null>(null);
  const [loading, setLoading] = useState(!publicPage);
  const [apps, setApps] = useState<Application[]>([]);
  const [appsLoading, setAppsLoading] = useState(false);
  const [appsError, setAppsError] = useState<string | null>(null);
  const [sessions, setSessions] = useState<Session[]>([]);
  const [sessionsLoading, setSessionsLoading] = useState(false);
  const [sessionsError, setSessionsError] = useState<string | null>(null);

  const refreshMe = useCallback(async () => {
    const current = await api<Me>("/api/v1/me");
    setMe(current);
    return current;
  }, []);

  useEffect(() => {
    document.documentElement.lang = "zh-CN";
    if (publicPage) {
      setLoading(false);
      return;
    }
    setLoading(true);
    void refreshMe()
      .catch(() => setMe(null))
      .finally(() => setLoading(false));
  }, [publicPage, refreshMe]);

  useEffect(() => {
    if (!me || me.mustChangePassword) {
      return;
    }
    setAppsLoading(true);
    void api<Application[]>("/api/v1/me/applications")
      .then((items) => {
        setApps(items);
        setAppsError(null);
      })
      .catch((err) => setAppsError((err as Error).message))
      .finally(() => setAppsLoading(false));
  }, [me]);

  const loadSessions = useCallback(async () => {
    setSessionsLoading(true);
    try {
      setSessions(await api<Session[]>("/api/v1/me/sessions"));
      setSessionsError(null);
    } catch (err) {
      setSessionsError((err as Error).message);
    } finally {
      setSessionsLoading(false);
    }
  }, []);

  if (publicPage) {
    return (
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/consent" element={<ConsentPage />} />
      </Routes>
    );
  }

  if (loading) {
    return <div className="loading">正在进入身份门户…</div>;
  }

  if (!me) {
    return <Navigate to="/login" replace />;
  }

  if (me.mustChangePassword) {
    return (
      <PasswordPage
        forced
        onSubmit={async (oldPassword, newPassword) => {
          await api("/api/v1/me/password", {
            method: "POST",
            body: JSON.stringify({ oldPassword, newPassword }),
          });
          await refreshMe();
        }}
      />
    );
  }

  return (
    <Routes>
      <Route element={<AppShell me={me} />}>
        <Route
          path="/"
          element={<PortalPage me={me} applications={apps} loading={appsLoading} error={appsError} />}
        />
        <Route
          path="/portal"
          element={<PortalPage me={me} applications={apps} loading={appsLoading} error={appsError} />}
        />
        <Route path="/profile" element={<ProfilePage me={me} />} />
        <Route
          path="/sessions"
          element={
            <SessionRoute
              sessions={sessions}
              loading={sessionsLoading}
              error={sessionsError}
              onLoad={loadSessions}
            />
          }
        />
        <Route
          path="/password"
          element={
            <PasswordPage
              onSubmit={async (oldPassword, newPassword) => {
                await api("/api/v1/me/password", {
                  method: "POST",
                  body: JSON.stringify({ oldPassword, newPassword }),
                });
              }}
            />
          }
        />
        <Route path="/examples/handbook" element={<HandbookPage />} />
        <Route path="/examples/directory" element={<DirectoryPage />} />
      </Route>
      <Route
        path="/admin"
        element={me.admin ? <AdminLayout me={me} /> : <Navigate to="/" replace />}
      >
        <Route index element={<AdminHome />} />
        <Route path="users" element={<AdminUsers />} />
        <Route path="users/new" element={<AdminUserEditor />} />
        <Route path="users/:id" element={<AdminUserEditor />} />
        <Route path="orgs" element={<AdminOrgs />} />
        <Route path="roles" element={<AdminRoles />} />
        <Route path="applications" element={<AdminApplications />} />
        <Route path="policies" element={<AdminPolicies />} />
        <Route path="import-jobs" element={<AdminImportJobs />} />
        <Route path="audit" element={<AdminAudit />} />
        <Route path="sessions" element={<AdminSessions />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

function SessionRoute({
  sessions,
  loading,
  error,
  onLoad,
}: {
  sessions: Session[];
  loading: boolean;
  error: string | null;
  onLoad: () => Promise<void>;
}) {
  useEffect(() => {
    void onLoad();
  }, [onLoad]);
  return (
    <SessionsPage
      sessions={sessions}
      loading={loading}
      error={error}
      onRevoke={async (id) => {
        await api(`/api/v1/me/sessions/${id}`, { method: "DELETE" });
        const current = sessions.find((item) => item.id === id);
        if (current?.current) {
          window.location.href = "/login?logout";
          return;
        }
        await onLoad();
      }}
      onRevokeOthers={async () => {
        await api("/api/v1/me/sessions/revoke-others", { method: "POST" });
        await onLoad();
      }}
    />
  );
}

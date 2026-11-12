import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import type { Application, Me } from "../api/types";

interface Props {
  me: Me;
  applications: Application[];
  loading: boolean;
  error: string | null;
}

export function PortalPage({ me, applications, loading, error }: Props) {
  const [query, setQuery] = useState("");
  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) {
      return applications;
    }
    return applications.filter((app) =>
      [app.name, app.code, app.description || ""].some((value) => value.toLowerCase().includes(q)),
    );
  }, [applications, query]);

  return (
    <div className="stack">
      <div className="page-head">
        <div>
          <h1>您好，{me.profile.displayName}</h1>
          <p className="lede">选择一个应用即可打开。门户会列出当前可供启动的系统。</p>
        </div>
        {me.admin ? (
          <Link className="btn" to="/admin">
            管理控制台
          </Link>
        ) : null}
      </div>
      <label className="field search" htmlFor="app-search">
        搜索应用
        <input
          id="app-search"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="按名称或编码查找"
        />
      </label>
      {loading ? <div className="loading">正在加载应用目录…</div> : null}
      {error ? (
        <div className="alert error" role="alert">
          {error}
        </div>
      ) : null}
      {!loading && applications.length === 0 ? (
        <div className="empty">
          <h2>暂时没有可打开的应用</h2>
          <p>当前没有已启用且配置了访问地址的应用。请联系管理员将应用加入门户。</p>
        </div>
      ) : null}
      {!loading && applications.length > 0 && filtered.length === 0 ? (
        <div className="empty">
          <h2>没有找到匹配的应用</h2>
          <p>试试其他关键词，或清空搜索后重新浏览。</p>
        </div>
      ) : null}
      <div className="grid">
        {filtered.map((app) => (
          <a
            key={app.id}
            className="card"
            href={app.launchUrl || "#"}
            target="_blank"
            rel="noreferrer noopener"
            style={{ ["--accent" as string]: app.accentColor || "#1f3a5f" }}
          >
            <div className="app-icon" aria-hidden>
              {(app.name || "?").slice(0, 1)}
            </div>
            <h3>{app.name}</h3>
            <p className="hint">{app.description || "打开此应用"}</p>
          </a>
        ))}
      </div>
    </div>
  );
}

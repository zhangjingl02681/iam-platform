import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { ensureCsrfToken, readCsrfToken } from "../api/client";

const SCOPE_LABELS: Record<string, string> = {
  openid: "确认您的身份（openid）",
  profile: "读取姓名与用户名等基本资料（profile）",
  email: "读取工作邮箱（email）",
  api: "访问受保护的接口（api）",
};

export function ConsentPage() {
  const [params] = useSearchParams();
  const [csrf, setCsrf] = useState(readCsrfToken());
  const clientId = params.get("client_id") || "";
  const state = params.get("state") || "";
  const scopes = useMemo(
    () => (params.get("scope") || "").split(/\s+/).filter(Boolean),
    [params],
  );

  useEffect(() => {
    document.documentElement.lang = "zh-CN";
    document.title = "授权许可 · 企业内部 IAM";
    void ensureCsrfToken().then(setCsrf);
  }, []);

  return (
    <div className="auth-shell">
      <aside className="auth-aside">
        <div>
          <div className="brand-mark">Enterprise IAM</div>
          <h2>确认访问</h2>
          <p>请确认是否允许该应用使用您的身份信息。您可以取消并返回。</p>
        </div>
      </aside>
      <main className="auth-main">
        <div className="panel">
          <h1>授权许可</h1>
          <p>
            应用 <strong>{clientId || "未知客户端"}</strong> 请求访问您的账号。
          </p>
          <form method="post" action="/oauth2/authorize">
            <input type="hidden" name="_csrf" value={csrf} />
            <input type="hidden" name="client_id" value={clientId} />
            <input type="hidden" name="state" value={state} />
            <fieldset className="stack" style={{ border: 0, padding: 0 }}>
              <legend className="hint">将授予以下权限</legend>
              {scopes.map((scope) => (
                <label key={scope} className="check">
                  <input type="checkbox" name="scope" value={scope} defaultChecked />
                  {SCOPE_LABELS[scope] || scope}
                </label>
              ))}
            </fieldset>
            <div className="btn-row" style={{ marginTop: 20 }}>
              <button type="submit" className="btn">
                允许
              </button>
              <button
                type="submit"
                className="btn secondary"
                name="deny"
                value="true"
                onClick={(event) => {
                  const form = event.currentTarget.form;
                  if (form) {
                    form.querySelectorAll('input[name="scope"]').forEach((input) => {
                      (input as HTMLInputElement).checked = false;
                    });
                  }
                }}
              >
                拒绝
              </button>
            </div>
          </form>
        </div>
      </main>
    </div>
  );
}

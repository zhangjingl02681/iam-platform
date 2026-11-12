import { FormEvent, useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { ensureCsrfToken, readCsrfToken, rememberedUsername, setRememberedUsername } from "../api/client";
import { PasswordInput } from "../components/PasswordInput";

const ERRORS: Record<string, string> = {
  bad_credentials: "用户名或密码不正确。请检查后重试。",
  locked: "账号已锁定，请稍后再试或联系管理员解锁。",
  disabled: "账号已停用，如需恢复请联系管理员。",
};

export function LoginPage() {
  const [params] = useSearchParams();
  const errorCode = params.get("error") || "";
  const loggedOut = params.has("logout");
  const remembered = useMemo(() => rememberedUsername() || "", []);
  const [username, setUsername] = useState(remembered);
  const [remember, setRemember] = useState(Boolean(remembered));
  const [csrf, setCsrf] = useState(readCsrfToken());

  useEffect(() => {
    document.documentElement.lang = "zh-CN";
    document.title = "登录 · 企业内部 IAM";
    void ensureCsrfToken().then((token) => setCsrf(token));
  }, []);

  function onRememberChange(checked: boolean) {
    setRemember(checked);
    setRememberedUsername(checked ? username : null);
  }

  function onUsernameChange(value: string) {
    setUsername(value);
    if (remember) {
      setRememberedUsername(value);
    }
  }

  function onSubmit(event: FormEvent<HTMLFormElement>) {
    if (remember) {
      setRememberedUsername(username);
    }
    if (!readCsrfToken() && !csrf) {
      event.preventDefault();
      void ensureCsrfToken().then((token) => {
        setCsrf(token);
        event.currentTarget?.submit();
      });
    }
  }

  const message = ERRORS[errorCode] || (errorCode ? ERRORS.bad_credentials : "");

  return (
    <div className="auth-shell">
      <aside className="auth-aside">
        <div>
          <div className="brand-mark">Enterprise IAM</div>
          <h1>企业内部身份门户</h1>
          <p className="lede">
            使用您的员工账号登录，进入应用门户、管理会话，或（如具备权限）进入管理控制台。
          </p>
        </div>
        <p>口令仅在本系统校验，不会出现在日志或接口响应中。</p>
      </aside>
      <main className="auth-main">
        <div className="panel">
          <h1>登录</h1>
          <p className="hint">请输入您的用户名和密码。</p>
          {loggedOut ? <div className="alert ok">您已安全退出。</div> : null}
          {message ? (
            <div className="alert error" role="alert">
              {message}
            </div>
          ) : null}
          <form method="post" action="/login" acceptCharset="UTF-8" onSubmit={onSubmit}>
            <input type="hidden" name="_csrf" value={csrf} />
            <label className="field" htmlFor="username">
              用户名
              <input
                id="username"
                name="username"
                autoComplete="username"
                autoFocus
                required
                value={username}
                onChange={(event) => onUsernameChange(event.target.value)}
              />
            </label>
            <PasswordInput id="password" name="password" label="密码" required autoComplete="current-password" />
            <label className="check">
              <input
                type="checkbox"
                checked={remember}
                onChange={(event) => onRememberChange(event.target.checked)}
              />
              记住用户名
            </label>
            <button type="submit" className="btn full-width">
              登录
            </button>
          </form>
        </div>
      </main>
    </div>
  );
}

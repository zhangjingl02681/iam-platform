import { Link } from "react-router-dom";
import { formatDateTime, statusLabel } from "../api/client";
import type { Me } from "../api/types";

export function ProfilePage({ me }: { me: Me }) {
  const profile = me.profile;
  return (
    <div className="split">
      <section className="panel" style={{ width: "auto" }}>
        <h1>个人资料</h1>
        <p className="lede">这些信息来自身份主数据，如需更正请联系管理员。</p>
        <dl className="stack">
          <div>
            <div className="hint">用户名</div>
            <div>{profile.username}</div>
          </div>
          <div>
            <div className="hint">显示名</div>
            <div>{profile.displayName}</div>
          </div>
          <div>
            <div className="hint">邮箱</div>
            <div>{profile.email || "—"}</div>
          </div>
          <div>
            <div className="hint">手机</div>
            <div>{profile.mobile || "—"}</div>
          </div>
          <div>
            <div className="hint">状态</div>
            <div>{statusLabel(profile.status)}</div>
          </div>
          <div>
            <div className="hint">最近登录</div>
            <div>{formatDateTime(profile.lastLoginAt)}</div>
          </div>
        </dl>
      </section>
      <aside className="stack">
        <div className="panel" style={{ width: "auto" }}>
          <h2>安全</h2>
          <p>定期更新口令，并检查仍保持登录的设备。</p>
          <div className="btn-row">
            <Link className="btn" to="/password">
              修改密码
            </Link>
            <Link className="btn secondary" to="/sessions">
              管理会话
            </Link>
          </div>
        </div>
      </aside>
    </div>
  );
}

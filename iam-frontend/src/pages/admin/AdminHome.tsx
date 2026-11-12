import { Link } from "react-router-dom";

export function AdminHome() {
  return (
    <div className="stack">
      <h1>管理控制台</h1>
      <p className="lede">维护企业内部员工身份、组织、应用入口与会话，并只读查看审计记录。</p>
      <div className="grid">
        <Link className="card" to="/admin/users">
          <h3>用户</h3>
          <p className="hint">检索、创建、停用或终止账号，重置口令并分配组织。用户不做物理删除。</p>
        </Link>
        <Link className="card" to="/admin/orgs">
          <h3>组织</h3>
          <p className="hint">维护组织树与排序。</p>
        </Link>
        <Link className="card" to="/admin/applications">
          <h3>应用目录</h3>
          <p className="hint">管理门户中展示的应用及启动地址。</p>
        </Link>
        <Link className="card" to="/admin/audit">
          <h3>审计事件</h3>
          <p className="hint">按时间、操作者和对象筛选只读事件流。</p>
        </Link>
      </div>
      <div className="note">
        多因素认证、特权账号（PAM）签出和 ABAC 策略决策将在后续迭代提供，当前不开放未完成的管理界面。
      </div>
    </div>
  );
}

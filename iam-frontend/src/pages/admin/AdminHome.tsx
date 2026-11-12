import { Link } from "react-router-dom";

export function AdminHome() {
  return (
    <div className="stack">
      <h1>管理控制台</h1>
      <p className="lede">维护企业内部员工身份、组织、角色、应用授权、访问策略与会话，并只读查看审计记录。</p>
      <div className="grid">
        <Link className="card" to="/admin/users">
          <h3>用户</h3>
          <p className="hint">检索、创建、停用或终止账号，重置口令并分配组织。用户不做物理删除。</p>
        </Link>
        <Link className="card" to="/admin/orgs">
          <h3>组织</h3>
          <p className="hint">维护组织树与排序。</p>
        </Link>
        <Link className="card" to="/admin/roles">
          <h3>角色</h3>
          <p className="hint">维护角色并在用户详情中分配。</p>
        </Link>
        <Link className="card" to="/admin/applications">
          <h3>应用目录</h3>
          <p className="hint">管理门户中展示的应用、启动地址与访问授权。</p>
        </Link>
        <Link className="card" to="/admin/policies">
          <h3>访问策略</h3>
          <p className="hint">编写、发布并绑定 JSON 策略，由决策点评估允许或拒绝。</p>
        </Link>
        <Link className="card" to="/admin/audit">
          <h3>审计事件</h3>
          <p className="hint">按时间、操作者和对象筛选只读事件流。</p>
        </Link>
      </div>
      <div className="note">
        多因素认证、特权账号（PAM）签出、Redis 会话和生产级密钥保险库将在后续迭代提供。
      </div>
    </div>
  );
}

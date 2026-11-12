export function HandbookPage() {
  return (
    <article className="panel" style={{ width: "auto", maxWidth: 720, margin: "0 auto" }}>
      <p className="brand-mark">示例应用</p>
      <h1>员工手册</h1>
      <p className="lede">
        这是门户中的示例应用，用于演示单点登录后如何打开内部系统。正式环境请将启动地址改为实际手册站点。
      </p>
      <h2>工作时间与出勤</h2>
      <p>请按所属组织公布的作息执行。请假与出差通过人事流程提前申请。</p>
      <h2>账号与安全</h2>
      <p>不要转借员工账号。离职或调岗时，权限由身份平台统一回收，无需在各系统逐一关闭。</p>
      <h2>求助渠道</h2>
      <p>账号锁定、无法登录或需要权限变更时，请联系身份管理员，不要将密码发送给任何人。</p>
    </article>
  );
}

export function DirectoryPage() {
  const rows = [
    { name: "系统管理员", org: "信息技术部", title: "平台管理" },
    { name: "示例同事", org: "人力资源部", title: "组织发展" },
    { name: "示例同事", org: "财务部", title: "费用审核" },
  ];
  return (
    <article>
      <p className="brand-mark">示例应用</p>
      <h1>员工通讯录</h1>
      <p className="lede">演示用通讯录，不代表真实人员。正式目录应连接权威组织数据。</p>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>姓名</th>
              <th>组织</th>
              <th>职位</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row, index) => (
              <tr key={index}>
                <td>{row.name}</td>
                <td>{row.org}</td>
                <td>{row.title}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </article>
  );
}

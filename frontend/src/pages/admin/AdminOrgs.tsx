import { FormEvent, useEffect, useState } from "react";
import { api, statusLabel } from "../../api/client";
import type { OrgNode } from "../../api/types";

export function AdminOrgs() {
  const [tree, setTree] = useState<OrgNode[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [form, setForm] = useState({ parentId: "", code: "", name: "", sortOrder: "0" });

  async function load() {
    try {
      setTree(await api<OrgNode[]>("/api/v1/orgs/tree"));
    } catch (err) {
      setError((err as Error).message);
    }
  }

  useEffect(() => {
    void load();
  }, []);

  async function create(event: FormEvent) {
    event.preventDefault();
    try {
      await api("/api/v1/orgs", {
        method: "POST",
        body: JSON.stringify({
          parentId: form.parentId || null,
          code: form.code,
          name: form.name,
          sortOrder: Number(form.sortOrder) || 0,
        }),
      });
      setForm({ parentId: "", code: "", name: "", sortOrder: "0" });
      setSuccess("组织已创建。");
      setError(null);
      await load();
    } catch (err) {
      setError((err as Error).message);
    }
  }

  return (
    <div className="split">
      <section>
        <h1>组织</h1>
        <p className="lede">维护企业组织树。子组织会缩进显示。</p>
        {error ? <div className="alert error">{error}</div> : null}
        {success ? <div className="alert ok">{success}</div> : null}
        <ul className="tree">
          {tree.map((node) => (
            <OrgItem key={node.id} node={node} />
          ))}
        </ul>
        {tree.length === 0 ? <div className="empty">还没有组织，请先创建一个根组织。</div> : null}
      </section>
      <form className="panel" style={{ width: "auto" }} onSubmit={(event) => void create(event)}>
        <h2>新建组织</h2>
        <label className="field">
          上级组织
          <select value={form.parentId} onChange={(e) => setForm({ ...form, parentId: e.target.value })}>
            <option value="">作为根组织</option>
            {flatten(tree).map((org) => (
              <option key={org.id} value={org.id}>
                {org.path || org.name}
              </option>
            ))}
          </select>
        </label>
        <label className="field">
          编码
          <input required value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} />
        </label>
        <label className="field">
          名称
          <input required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
        </label>
        <label className="field">
          排序
          <input type="number" value={form.sortOrder} onChange={(e) => setForm({ ...form, sortOrder: e.target.value })} />
        </label>
        <button className="btn" type="submit">
          创建
        </button>
      </form>
    </div>
  );
}

function OrgItem({ node }: { node: OrgNode }) {
  return (
    <li>
      <div className="node">
        <strong>{node.name}</strong>
        <span className="hint">{node.code}</span>
        <span className="badge">{statusLabel(node.status)}</span>
      </div>
      {node.children && node.children.length > 0 ? (
        <ul>
          {node.children.map((child) => (
            <OrgItem key={child.id} node={child} />
          ))}
        </ul>
      ) : null}
    </li>
  );
}

function flatten(nodes: OrgNode[], acc: OrgNode[] = []): OrgNode[] {
  for (const node of nodes) {
    acc.push(node);
    if (node.children) flatten(node.children, acc);
  }
  return acc;
}

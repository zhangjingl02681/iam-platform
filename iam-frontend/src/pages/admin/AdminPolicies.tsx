import { FormEvent, useEffect, useState } from "react";
import { api, statusLabel } from "../../api/client";
import type { PageResponse, Policy, PolicyVersion } from "../../api/types";

const emptyDocument = `{
  "effect": "allow",
  "actions": ["app:launch"],
  "resources": [{ "type": "application", "id": "*" }],
  "when": { "roles": [], "orgCodes": [], "attributes": {} }
}`;

export function AdminPolicies() {
  const [page, setPage] = useState<PageResponse<Policy> | null>(null);
  const [selected, setSelected] = useState<Policy | null>(null);
  const [versions, setVersions] = useState<PolicyVersion[]>([]);
  const [code, setCode] = useState("");
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [document, setDocument] = useState(emptyDocument);
  const [targetType, setTargetType] = useState("application");
  const [targetId, setTargetId] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  async function load() {
    setPage(await api<PageResponse<Policy>>("/api/v1/policies?size=50"));
  }

  async function loadVersions(policyId: string) {
    setVersions(await api<PolicyVersion[]>(`/api/v1/policies/${policyId}/versions`));
  }

  useEffect(() => {
    void load().catch((err) => setError((err as Error).message));
  }, []);

  async function select(policy: Policy) {
    setSelected(policy);
    setCode(policy.code);
    setName(policy.name);
    setDescription(policy.description || "");
    await loadVersions(policy.id);
  }

  async function create(event: FormEvent) {
    event.preventDefault();
    setError(null);
    try {
      const policy = await api<Policy>("/api/v1/policies", {
        method: "POST",
        body: JSON.stringify({ code, name, description }),
      });
      await api(`/api/v1/policies/${policy.id}/versions`, {
        method: "POST",
        body: JSON.stringify({ document, language: "json" }),
      });
      setSelected(policy);
      setSuccess("策略已创建，版本已保存为草稿。");
      await load();
      await loadVersions(policy.id);
    } catch (err) {
      setError((err as Error).message);
    }
  }

  async function publish(version: PolicyVersion) {
    if (!selected) return;
    try {
      await api(`/api/v1/policies/${selected.id}/versions/${version.id}/publish`, { method: "POST" });
      setSuccess("策略版本已发布。");
      await load();
      await loadVersions(selected.id);
    } catch (err) {
      setError((err as Error).message);
    }
  }

  async function bind(event: FormEvent) {
    event.preventDefault();
    if (!selected) return;
    const published = versions.filter((item) => item.status === "PUBLISHED").at(-1);
    if (!published) {
      setError("请先发布一个策略版本，再绑定目标。");
      return;
    }
    try {
      await api(`/api/v1/policies/${selected.id}/bindings`, {
        method: "POST",
        body: JSON.stringify({ versionId: published.id, targetType, targetId }),
      });
      setSuccess("策略已绑定到目标。");
    } catch (err) {
      setError((err as Error).message);
    }
  }

  return (
    <div className="split">
      <section>
        <h1>访问策略</h1>
        <p className="lede">
          使用 JSON 策略文档控制操作授权。拒绝优先于允许；没有匹配策略时，决策接口默认拒绝。门户应用会先按授权过滤，若已有已发布的 app:launch
          策略作用于该应用，还需策略允许。
        </p>
        {error ? <div className="alert error">{error}</div> : null}
        {success ? <div className="alert ok">{success}</div> : null}
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>名称</th>
                <th>编码</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              {page?.content.map((policy) => (
                <tr key={policy.id}>
                  <td>
                    <button type="button" className="btn ghost" onClick={() => void select(policy)}>
                      {policy.name}
                    </button>
                  </td>
                  <td className="mono">{policy.code}</td>
                  <td>{statusLabel(policy.status)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {selected ? (
          <div className="panel" style={{ width: "auto", marginTop: 16 }}>
            <h2>版本</h2>
            {versions.map((version) => (
              <div key={version.id} className="btn-row" style={{ marginBottom: 8 }}>
                <span>
                  v{version.version} · {statusLabel(version.status)}
                </span>
                {version.status !== "PUBLISHED" ? (
                  <button type="button" className="btn secondary" onClick={() => void publish(version)}>
                    发布
                  </button>
                ) : (
                  <span className="badge ok">已发布</span>
                )}
              </div>
            ))}
            <form onSubmit={(event) => void bind(event)}>
              <label className="field">
                绑定目标类型
                <select value={targetType} onChange={(e) => setTargetType(e.target.value)}>
                  <option value="application">应用</option>
                  <option value="org">组织</option>
                </select>
              </label>
              <label className="field">
                目标 ID
                <input value={targetId} onChange={(e) => setTargetId(e.target.value)} />
              </label>
              <button className="btn secondary" type="submit">
                绑定已发布版本
              </button>
            </form>
          </div>
        ) : null}
      </section>
      <form className="panel" style={{ width: "auto" }} onSubmit={(event) => void create(event)}>
        <h2>新建策略</h2>
        <label className="field">
          编码
          <input required value={code} onChange={(e) => setCode(e.target.value)} />
        </label>
        <label className="field">
          名称
          <input required value={name} onChange={(e) => setName(e.target.value)} />
        </label>
        <label className="field">
          描述
          <textarea value={description} onChange={(e) => setDescription(e.target.value)} />
        </label>
        <label className="field">
          策略文档
          <textarea rows={12} value={document} onChange={(e) => setDocument(e.target.value)} required />
        </label>
        <button className="btn" type="submit">
          创建并保存版本
        </button>
      </form>
    </div>
  );
}

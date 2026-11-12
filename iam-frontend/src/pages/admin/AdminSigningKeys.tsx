import { useEffect, useState } from "react";
import { api, formatDateTime, statusLabel } from "../../api/client";
import type { SigningKey } from "../../api/types";

export function AdminSigningKeys() {
  const [keys, setKeys] = useState<SigningKey[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  async function load() {
    setKeys(await api<SigningKey[]>("/api/v1/signing-keys"));
  }

  useEffect(() => {
    void load().catch((err) => setError((err as Error).message));
  }, []);

  async function rotate() {
    setError(null);
    try {
      await api("/api/v1/signing-keys/rotate", { method: "POST" });
      setSuccess("已轮换签名密钥。新令牌使用新密钥签发，上一把密钥在宽限期或退役前仍可用于校验。");
      await load();
    } catch (err) {
      setError((err as Error).message);
    }
  }

  async function retire(kid: string) {
    setError(null);
    try {
      await api(`/api/v1/signing-keys/${kid}/retire`, { method: "POST" });
      setSuccess("已退役上一把密钥，使用它签发的令牌将无法再通过校验。");
      await load();
    } catch (err) {
      setError((err as Error).message);
    }
  }

  return (
    <section>
      <h1>签名密钥</h1>
      <p className="lede">
        私钥保存在本地保险库目录，不进入业务库或接口响应。生产环境应改为 HSM / 云 KMS，本页只提供开发级轮换。
      </p>
      {error ? <div className="alert error">{error}</div> : null}
      {success ? <div className="alert ok">{success}</div> : null}
      <div className="btn-row" style={{ marginBottom: "1rem" }}>
        <button type="button" className="btn" onClick={() => void rotate()}>
          轮换密钥
        </button>
      </div>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>kid</th>
              <th>状态</th>
              <th>创建时间</th>
              <th>有效至</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {keys.map((key) => (
              <tr key={key.kid}>
                <td className="mono">{key.kid}</td>
                <td>{statusLabel(key.status)}</td>
                <td>{formatDateTime(key.createdAt)}</td>
                <td>{formatDateTime(key.validUntil)}</td>
                <td>
                  {key.status === "PREVIOUS" ? (
                    <button type="button" className="btn secondary" onClick={() => void retire(key.kid)}>
                      退役
                    </button>
                  ) : null}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

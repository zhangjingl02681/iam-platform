import { FormEvent, useState } from "react";
import { PasswordInput } from "../components/PasswordInput";
import type { ApiError } from "../api/types";

interface Props {
  forced?: boolean;
  onSubmit: (oldPassword: string, newPassword: string) => Promise<void>;
}

export function PasswordPage({ forced = false, onSubmit }: Props) {
  const [oldPassword, setOldPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setSuccess(null);
    if (newPassword !== confirm) {
      setError("两次输入的新密码不一致。");
      return;
    }
    setPending(true);
    try {
      await onSubmit(oldPassword, newPassword);
      setError(null);
      setSuccess("密码已更新。");
      setOldPassword("");
      setNewPassword("");
      setConfirm("");
    } catch (err) {
      const api = err as ApiError;
      setError(api.detail || api.title || "无法更新密码，请检查输入后重试。");
    } finally {
      setPending(false);
    }
  }

  return (
    <div className="panel" style={{ margin: forced ? "8vh auto" : 0 }}>
      <h1>{forced ? "请先修改密码" : "修改密码"}</h1>
      {forced ? (
        <p>管理员要求您在继续使用之前设置新密码。完成后再进入门户。</p>
      ) : (
        <p className="lede">更新登录口令。新密码立即生效，其他设备上的会话可能需要重新登录。</p>
      )}
      <ul className="hint">
        <li>长度须为 12–128 个字符</li>
        <li>至少包含一个字母和一个数字</li>
        <li>不能与最近使用过的密码相同</li>
      </ul>
      {error ? (
        <div className="alert error" role="alert">
          {error}
        </div>
      ) : null}
      {success ? <div className="alert ok">{success}</div> : null}
      <form onSubmit={handleSubmit}>
        <PasswordInput
          id="oldPassword"
          label="当前密码"
          value={oldPassword}
          onChange={setOldPassword}
          required
          autoComplete="current-password"
        />
        <PasswordInput
          id="newPassword"
          label="新密码"
          value={newPassword}
          onChange={setNewPassword}
          required
          autoComplete="new-password"
        />
        <PasswordInput
          id="confirmPassword"
          label="确认新密码"
          value={confirm}
          onChange={setConfirm}
          required
          autoComplete="new-password"
        />
        <button type="submit" className="btn" disabled={pending}>
          保存新密码
        </button>
      </form>
    </div>
  );
}

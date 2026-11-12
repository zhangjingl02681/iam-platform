import { useState } from "react";

interface Props {
  id: string;
  name?: string;
  label: string;
  value?: string;
  required?: boolean;
  autoComplete?: string;
  onChange?: (value: string) => void;
}

export function PasswordInput({ id, name, label, value, required, autoComplete, onChange }: Props) {
  const [visible, setVisible] = useState(false);
  return (
    <label className="field" htmlFor={id}>
      <span className="field-label">{label}</span>
      <div className="password-field">
        <input
          id={id}
          name={name}
          type={visible ? "text" : "password"}
          required={required}
          autoComplete={autoComplete}
          value={value}
          onChange={onChange ? (event) => onChange(event.target.value) : undefined}
        />
        <button
          type="button"
          className="btn ghost"
          onClick={() => setVisible((v) => !v)}
          aria-label={visible ? "隐藏密码" : "显示密码"}
        >
          {visible ? "隐藏" : "显示"}
        </button>
      </div>
    </label>
  );
}

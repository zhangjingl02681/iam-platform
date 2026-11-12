interface Props {
  open: boolean;
  title: string;
  message: string;
  confirmLabel?: string;
  danger?: boolean;
  onConfirm: () => void;
  onClose: () => void;
}

export function ConfirmDialog({
  open,
  title,
  message,
  confirmLabel = "确认",
  danger = true,
  onConfirm,
  onClose,
}: Props) {
  if (!open) {
    return null;
  }
  return (
    <div className="dialog-backdrop" onClick={onClose}>
      <div
        className="dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="confirm-title"
        onClick={(event) => event.stopPropagation()}
      >
        <h2 id="confirm-title">{title}</h2>
        <p>{message}</p>
        <div className="btn-row">
          <button type="button" className="btn secondary" onClick={onClose}>
            取消
          </button>
          <button type="button" className={`btn ${danger ? "danger" : ""}`} onClick={onConfirm}>
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}

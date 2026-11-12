import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { ConfirmDialog } from "../ConfirmDialog";

describe("ConfirmDialog", () => {
  it("does not confirm until the user accepts, and can be cancelled", async () => {
    const user = userEvent.setup();
    const onConfirm = vi.fn();
    const onClose = vi.fn();
    render(
      <ConfirmDialog
        open
        title="终止账号"
        message="终止后不可恢复登录，账号记录将保留。"
        confirmLabel="确认终止"
        onConfirm={onConfirm}
        onClose={onClose}
      />,
    );
    expect(screen.getByRole("dialog")).toHaveAccessibleName("终止账号");
    await user.click(screen.getByRole("button", { name: "取消" }));
    expect(onClose).toHaveBeenCalled();
    expect(onConfirm).not.toHaveBeenCalled();
  });
});

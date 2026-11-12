import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { PasswordPage } from "../PasswordPage";

describe("PasswordPage", () => {
  it("explains the password policy and surfaces API validation messages", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockRejectedValue({
      status: 400,
      code: "password_policy",
      detail: "Password must be between 12 and 128 characters",
    });
    render(<PasswordPage forced onSubmit={onSubmit} />);
    expect(screen.getByRole("heading", { name: /修改密码/ })).toBeInTheDocument();
    expect(screen.getByText(/12–128/)).toBeInTheDocument();
    expect(screen.getByText(/字母/)).toBeInTheDocument();
    expect(screen.getByText(/不能与最近使用过的密码相同/)).toBeInTheDocument();
    await user.type(screen.getByLabelText("当前密码"), "Initial-Passw0rd!");
    await user.type(screen.getByLabelText("新密码"), "short");
    await user.type(screen.getByLabelText("确认新密码"), "short");
    await user.click(screen.getByRole("button", { name: "保存新密码" }));
    expect(onSubmit).toHaveBeenCalled();
    expect(await screen.findByRole("alert")).toHaveTextContent(/12 and 128|12–128|12-128|字符/);
  });
});

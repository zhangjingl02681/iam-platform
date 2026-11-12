import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { LoginPage } from "../LoginPage";

function renderLogin(search = "") {
  return render(
    <MemoryRouter initialEntries={[`/login${search}`]}>
      <LoginPage />
    </MemoryRouter>,
  );
}

describe("LoginPage", () => {
  it("renders a Chinese login form with autofocus and posts to /login", () => {
    renderLogin();
    expect(document.documentElement.lang).toBe("zh-CN");
    expect(screen.getByRole("heading", { name: /登录/ })).toBeInTheDocument();
    const form = document.querySelector("form");
    expect(form).toHaveAttribute("method", "post");
    expect(form).toHaveAttribute("action", "/login");
    const username = screen.getByLabelText("用户名");
    expect(username).toHaveAttribute("name", "username");
    expect(username).toHaveFocus();
    expect(screen.getByLabelText("密码")).toHaveAttribute("name", "password");
    expect(screen.getByRole("button", { name: "登录" })).toBeInTheDocument();
    expect(document.querySelector('input[name="_csrf"]')).toBeInTheDocument();
  });

  it("shows safe, specific errors without revealing whether a username exists", () => {
    renderLogin("?error=bad_credentials");
    expect(screen.getByRole("alert")).toHaveTextContent("用户名或密码不正确");

    renderLogin("?error=locked");
    expect(screen.getAllByRole("alert").at(-1)).toHaveTextContent("账号已锁定");

    renderLogin("?error=disabled");
    expect(screen.getAllByRole("alert").at(-1)).toHaveTextContent("账号已停用");
  });

  it("toggles password visibility and can remember the username", async () => {
    const user = userEvent.setup();
    renderLogin();
    const password = screen.getByLabelText("密码");
    expect(password).toHaveAttribute("type", "password");
    await user.click(screen.getByRole("button", { name: "显示密码" }));
    expect(password).toHaveAttribute("type", "text");
    await user.click(screen.getByRole("button", { name: "隐藏密码" }));
    expect(password).toHaveAttribute("type", "password");

    await user.type(screen.getByLabelText("用户名"), "alice");
    await user.click(screen.getByLabelText("记住用户名"));
    expect(localStorage.getItem("iam.rememberedUsername")).toBe("alice");
  });
});

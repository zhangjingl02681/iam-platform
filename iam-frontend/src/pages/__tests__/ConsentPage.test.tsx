import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { ConsentPage } from "../ConsentPage";

describe("ConsentPage", () => {
  it("lists requested scopes and posts consent back to the authorization endpoint", () => {
    render(
      <MemoryRouter
        initialEntries={["/consent?client_id=iam-demo-consent&state=abc123&scope=openid%20profile"]}
      >
        <ConsentPage />
      </MemoryRouter>,
    );
    expect(screen.getByRole("heading", { name: /授权许可/ })).toBeInTheDocument();
    expect(screen.getByText(/iam-demo-consent/)).toBeInTheDocument();
    const form = document.querySelector("form");
    expect(form).toHaveAttribute("action", "/oauth2/authorize");
    expect(form).toHaveAttribute("method", "post");
    expect(screen.getByText(/openid/)).toBeInTheDocument();
    expect(screen.getByText(/profile/)).toBeInTheDocument();
    expect(document.querySelectorAll('input[name="scope"]').length).toBeGreaterThanOrEqual(2);
    expect(document.querySelector('input[name="client_id"]')).toHaveValue("iam-demo-consent");
    expect(document.querySelector('input[name="state"]')).toHaveValue("abc123");
  });
});

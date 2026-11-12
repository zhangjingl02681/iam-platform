import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { vi } from "vitest";
import { ProfilePage } from "../ProfilePage";
import type { Me, MfaEnrollment, MfaFactor } from "../../api/types";

vi.mock("../../api/client", async () => {
  const actual = await vi.importActual<typeof import("../../api/client")>("../../api/client");
  return { ...actual, api: vi.fn() };
});

import { api } from "../../api/client";

const mockedApi = api as unknown as ReturnType<typeof vi.fn>;

const me: Me = {
  profile: {
    id: "u1",
    username: "alice",
    displayName: "Alice",
    status: "ACTIVE",
  },
  mustChangePassword: false,
  admin: false,
  authorities: [],
};

describe("ProfilePage", () => {
  beforeEach(() => {
    mockedApi.mockReset();
  });

  it("enrolls a TOTP factor and shows the otpauth URI and recovery codes", async () => {
    const user = userEvent.setup();
    const enrollment: MfaEnrollment = {
      id: "f1",
      type: "totp",
      label: "Authenticator",
      secret: "ABCDEFG",
      otpauthUri: "otpauth://totp/Enterprise%20IAM:alice?secret=ABCDEFG",
    };
    const factor: MfaFactor = { id: "f1", type: "totp", label: "Authenticator", verifiedAt: "2024-01-01T00:00:00Z" };
    mockedApi.mockImplementation(async (path: string, init?: RequestInit) => {
      if (path === "/api/v1/me/mfa/factors" && !init?.method) {
        return mockedApi.mock.calls.some((call) => String(call[0]).includes("/confirm")) ? [factor] : [];
      }
      if (path === "/api/v1/me/mfa/totp/enrollments" && init?.method === "POST") {
        return enrollment;
      }
      if (String(path).includes("/confirm")) {
        return { factor, recoveryCodes: ["AAAA-BBBB"] };
      }
      return [];
    });

    render(
      <MemoryRouter>
        <ProfilePage me={me} />
      </MemoryRouter>,
    );

    expect(await screen.findByRole("heading", { name: "双因素认证" })).toBeInTheDocument();
    await user.click(await screen.findByRole("button", { name: "启用身份验证器" }));
    expect(await screen.findByText(/otpauth:\/\/totp/)).toBeInTheDocument();
    expect(screen.getByText("ABCDEFG")).toBeInTheDocument();
    await user.type(screen.getByLabelText("验证码"), "123456");
    await user.click(screen.getByRole("button", { name: "确认启用" }));
    expect(await screen.findByText("AAAA-BBBB")).toBeInTheDocument();
    expect(await screen.findByText(/已验证/)).toBeInTheDocument();
  });
});

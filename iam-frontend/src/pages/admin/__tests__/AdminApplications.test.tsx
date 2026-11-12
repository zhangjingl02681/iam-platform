import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { vi } from "vitest";
import { AdminApplications } from "../AdminApplications";
import type { Application, Entitlement, PageResponse } from "../../../api/types";

vi.mock("../../../api/client", async () => {
  const actual = await vi.importActual<typeof import("../../../api/client")>("../../../api/client");
  return { ...actual, api: vi.fn() };
});

import { api } from "../../../api/client";

const mockedApi = api as unknown as ReturnType<typeof vi.fn>;

const app: Application = {
  id: "app-1",
  code: "finance",
  name: "财务系统",
  status: "ACTIVE",
  launchUrl: "/examples/finance",
};

describe("AdminApplications", () => {
  beforeEach(() => {
    mockedApi.mockReset();
  });

  it("lets an administrator list and add entitlements on an application", async () => {
    const user = userEvent.setup();
    const grant: Entitlement = {
      id: "ent-1",
      applicationId: "app-1",
      subjectType: "USER",
      subjectId: "user-1",
    };
    mockedApi.mockImplementation(async (path: string, init?: RequestInit) => {
      if (path.startsWith("/api/v1/applications?") || path === "/api/v1/applications?size=50") {
        const page: PageResponse<Application> = {
          content: [app],
          page: 0,
          size: 50,
          totalElements: 1,
          totalPages: 1,
        };
        return page;
      }
      if (path === "/api/v1/applications/app-1/entitlements" && init?.method === "POST") {
        return grant;
      }
      if (path === "/api/v1/applications/app-1/entitlements") {
        return mockedApi.mock.calls.some((call) => call[1]?.method === "POST") ? [grant] : [];
      }
      return app;
    });

    render(<AdminApplications />);
    await user.click(await screen.findByRole("button", { name: "财务系统" }));
    expect(await screen.findByRole("heading", { name: "访问授权" })).toBeInTheDocument();
    expect(screen.getByText(/没有授权记录时，所有已登录用户都可以在门户中看到该应用/)).toBeInTheDocument();

    await user.selectOptions(screen.getByLabelText("主体类型"), "USER");
    await user.type(screen.getByLabelText("主体 ID"), "user-1");
    await user.click(screen.getByRole("button", { name: "添加授权" }));
    expect(await screen.findByText("user-1")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "移除" })).toBeInTheDocument();
  });
});

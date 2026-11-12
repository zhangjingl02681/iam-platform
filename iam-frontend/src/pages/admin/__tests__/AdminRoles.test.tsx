import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { vi } from "vitest";
import { AdminRoles } from "../AdminRoles";
import type { PageResponse, Role } from "../../../api/types";

vi.mock("../../../api/client", async () => {
  const actual = await vi.importActual<typeof import("../../../api/client")>("../../../api/client");
  return { ...actual, api: vi.fn() };
});

import { api } from "../../../api/client";

const mockedApi = api as unknown as ReturnType<typeof vi.fn>;

describe("AdminRoles", () => {
  beforeEach(() => {
    mockedApi.mockReset();
  });

  it("lists roles and can create a new role", async () => {
    const user = userEvent.setup();
    const admin: Role = {
      id: "role-admin",
      code: "admin",
      name: "管理员",
      status: "ACTIVE",
    };
    const created: Role = {
      id: "role-fin",
      code: "finance",
      name: "财务",
      description: "财务人员",
      status: "ACTIVE",
    };
    let roles = [admin];
    mockedApi.mockImplementation(async (_path: string, init?: RequestInit) => {
      if (init?.method === "POST") {
        roles = [admin, created];
        return created;
      }
      const page: PageResponse<Role> = {
        content: roles,
        page: 0,
        size: 50,
        totalElements: roles.length,
        totalPages: 1,
      };
      return page;
    });

    render(
      <MemoryRouter>
        <AdminRoles />
      </MemoryRouter>,
    );
    expect(await screen.findByRole("heading", { name: "角色" })).toBeInTheDocument();
    expect(screen.getByText("管理员")).toBeInTheDocument();

    await user.type(screen.getByLabelText("编码"), "finance");
    await user.type(screen.getByLabelText("名称"), "财务");
    await user.type(screen.getByLabelText("描述"), "财务人员");
    await user.click(screen.getByRole("button", { name: "保存" }));

    expect(await screen.findByText("财务")).toBeInTheDocument();
    expect(screen.getByText("finance")).toBeInTheDocument();
  });
});

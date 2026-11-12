import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { vi } from "vitest";
import { AdminPam } from "../AdminPam";
import type { PamRequestItem, PamTarget } from "../../../api/types";

vi.mock("../../../api/client", async () => {
  const actual = await vi.importActual<typeof import("../../../api/client")>("../../../api/client");
  return { ...actual, api: vi.fn() };
});

import { api } from "../../../api/client";

const mockedApi = api as unknown as ReturnType<typeof vi.fn>;

describe("AdminPam", () => {
  beforeEach(() => {
    mockedApi.mockReset();
  });

  it("registers a target and can approve a pending request", async () => {
    const user = userEvent.setup();
    const target: PamTarget = {
      id: "t1",
      type: "host",
      name: "prod-db",
      address: "10.1.2.3",
      env: "prod",
      status: "ACTIVE",
    };
    const request: PamRequestItem = {
      id: "r1",
      accountId: "a1",
      requesterId: "u1",
      reason: "紧急检修",
      startAt: "2024-01-01T00:00:00Z",
      endAt: "2024-01-01T02:00:00Z",
      status: "PENDING",
    };
    let targets: PamTarget[] = [];
    let requests = [request];
    mockedApi.mockImplementation(async (path: string, init?: RequestInit) => {
      if (path === "/api/v1/pam/targets" && init?.method === "POST") {
        targets = [target];
        return target;
      }
      if (path === "/api/v1/pam/targets") {
        return targets;
      }
      if (path.includes("/accounts") && !init?.method) {
        return [];
      }
      if (path === "/api/v1/pam/requests") {
        return requests;
      }
      if (String(path).endsWith("/approve") && init?.method === "POST") {
        requests = [{ ...request, status: "APPROVED" }];
        return requests[0];
      }
      return [];
    });

    render(
      <MemoryRouter>
        <AdminPam />
      </MemoryRouter>,
    );
    expect(await screen.findByRole("heading", { name: "特权访问管理" })).toBeInTheDocument();
    expect(await screen.findByText("紧急检修")).toBeInTheDocument();

    await user.type(screen.getByLabelText("名称"), "prod-db");
    await user.type(screen.getByLabelText("地址"), "10.1.2.3");
    await user.click(screen.getByRole("button", { name: "保存目标" }));
    expect(await screen.findByText("prod-db")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "批准" }));
    expect(await screen.findByText("已批准")).toBeInTheDocument();
  });
});

import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { vi } from "vitest";
import { AdminPolicies } from "../AdminPolicies";
import type { PageResponse, Policy, PolicyVersion } from "../../../api/types";

vi.mock("../../../api/client", async () => {
  const actual = await vi.importActual<typeof import("../../../api/client")>("../../../api/client");
  return { ...actual, api: vi.fn() };
});

import { api } from "../../../api/client";

const mockedApi = api as unknown as ReturnType<typeof vi.fn>;

describe("AdminPolicies", () => {
  beforeEach(() => {
    mockedApi.mockReset();
  });

  it("creates, lists and publishes a policy version", async () => {
    const user = userEvent.setup();
    const policy: Policy = {
      id: "pol-1",
      code: "allow-finance",
      name: "财务可启动",
      status: "DRAFT",
    };
    const version: PolicyVersion = {
      id: "ver-1",
      policyId: "pol-1",
      version: 1,
      document: '{"effect":"allow","actions":["app:launch"],"resources":[{"type":"application","id":"*"}]}',
      checksum: "abc",
      status: "DRAFT",
    };
    let policies: Policy[] = [];
    let versions: PolicyVersion[] = [];
    mockedApi.mockImplementation(async (path: string, init?: RequestInit) => {
      if (path === "/api/v1/policies" && init?.method === "POST") {
        policies = [policy];
        return policy;
      }
      if (path === "/api/v1/policies/pol-1/versions" && init?.method === "POST") {
        versions = [version];
        return version;
      }
      if (path === "/api/v1/policies/pol-1/versions/ver-1/publish") {
        const published = { ...version, status: "PUBLISHED" };
        versions = [published];
        policies = [{ ...policy, status: "PUBLISHED" }];
        return published;
      }
      if (path.startsWith("/api/v1/policies/pol-1/versions")) {
        return versions;
      }
      const page: PageResponse<Policy> = {
        content: policies,
        page: 0,
        size: 50,
        totalElements: policies.length,
        totalPages: 1,
      };
      return page;
    });

    render(<AdminPolicies />);
    expect(await screen.findByRole("heading", { name: "访问策略" })).toBeInTheDocument();

    await user.type(screen.getByLabelText("编码"), "allow-finance");
    await user.type(screen.getByLabelText("名称"), "财务可启动");
    expect((screen.getByLabelText("策略文档") as HTMLTextAreaElement).value).toContain("app:launch");
    await user.click(screen.getByRole("button", { name: "创建并保存版本" }));

    expect(await screen.findByText("财务可启动")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "发布" }));
    expect((await screen.findAllByText("已发布")).length).toBeGreaterThan(0);
  });
});

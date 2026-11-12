import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { vi } from "vitest";
import { AdminImportJobs } from "../AdminImportJobs";
import type { ImportJob, PageResponse } from "../../../api/types";

vi.mock("../../../api/client", async () => {
  const actual = await vi.importActual<typeof import("../../../api/client")>("../../../api/client");
  return { ...actual, api: vi.fn() };
});

import { api } from "../../../api/client";

const mockedApi = api as unknown as ReturnType<typeof vi.fn>;

const emptyPage: PageResponse<ImportJob> = {
  content: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
};

describe("AdminImportJobs", () => {
  beforeEach(() => {
    mockedApi.mockReset();
  });

  it("uploads a CSV file and shows row counts and the error report", async () => {
    const user = userEvent.setup();
    const completed: ImportJob = {
      id: "job-1",
      type: "user_csv",
      fileRef: "users.csv",
      status: "SUCCEEDED",
      total: 4,
      successCount: 2,
      failedCount: 2,
      errorReport: "row 3: Organization not found: missing-org",
      createdAt: "2026-11-12T10:00:00+08:00",
      finishedAt: "2026-11-12T10:00:01+08:00",
    };
    mockedApi.mockImplementation(async (_path: string, init?: RequestInit) => {
      if (init?.method === "POST") {
        expect(init.body).toBeInstanceOf(FormData);
        const body = init.body as FormData;
        expect(body.get("type")).toBe("user_csv");
        expect(body.get("file")).toBeInstanceOf(File);
        return completed;
      }
      return { ...emptyPage, content: init ? [completed] : [], totalElements: init ? 1 : 0, totalPages: 1 };
    });

    render(<AdminImportJobs />);
    expect(await screen.findByRole("heading", { name: "导入任务" })).toBeInTheDocument();
    expect(screen.getByLabelText("CSV 文件")).toBeInTheDocument();

    const file = new File(
      ["username,display_name\nnewuser,新用户\n"],
      "users.csv",
      { type: "text/csv" },
    );
    await user.upload(screen.getByLabelText("CSV 文件"), file);
    await user.click(screen.getByRole("button", { name: "上传并导入" }));

    expect(await screen.findByText(/成功 2/)).toBeInTheDocument();
    expect(screen.getByText(/失败 2/)).toBeInTheDocument();
    expect(screen.getByText(/Organization not found: missing-org/)).toBeInTheDocument();
    expect(screen.queryByText(/不解析上传文件/)).not.toBeInTheDocument();
  });
});

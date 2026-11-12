import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { vi } from "vitest";
import { AdminSigningKeys } from "../AdminSigningKeys";
import type { SigningKey } from "../../../api/types";

vi.mock("../../../api/client", async () => {
  const actual = await vi.importActual<typeof import("../../../api/client")>("../../../api/client");
  return { ...actual, api: vi.fn() };
});

import { api } from "../../../api/client";

const mockedApi = api as unknown as ReturnType<typeof vi.fn>;

describe("AdminSigningKeys", () => {
  beforeEach(() => {
    mockedApi.mockReset();
  });

  it("lists key metadata without private material and can rotate", async () => {
    const user = userEvent.setup();
    let keys: SigningKey[] = [
      { kid: "kid-old", status: "CURRENT", createdAt: "2024-01-01T00:00:00Z" },
    ];
    mockedApi.mockImplementation(async (path: string, init?: RequestInit) => {
      if (path === "/api/v1/signing-keys/rotate" && init?.method === "POST") {
        keys = [
          { kid: "kid-new", status: "CURRENT", createdAt: "2024-02-01T00:00:00Z" },
          { kid: "kid-old", status: "PREVIOUS", createdAt: "2024-01-01T00:00:00Z" },
        ];
        return { current: keys[0], previous: keys[1] };
      }
      return keys;
    });

    render(
      <MemoryRouter>
        <AdminSigningKeys />
      </MemoryRouter>,
    );
    expect(await screen.findByRole("heading", { name: "签名密钥" })).toBeInTheDocument();
    expect(screen.getByText("kid-old")).toBeInTheDocument();
    expect(screen.queryByText(/BEGIN RSA/)).not.toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "轮换密钥" }));
    expect(await screen.findByText("kid-new")).toBeInTheDocument();
    expect(screen.getByText("上一把")).toBeInTheDocument();
  });
});

import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { SessionsPage } from "../SessionsPage";
import type { Session } from "../../api/types";

const sessions: Session[] = [
  {
    id: "s-current",
    userId: "u1",
    username: "zhang",
    ip: "10.0.0.8",
    userAgent: "Mozilla/5.0",
    amr: "pwd",
    createdAt: "2026-11-12T10:00:00Z",
    lastSeenAt: "2026-11-12T10:30:00Z",
    idleExpiresAt: "2026-11-12T11:00:00Z",
    absExpiresAt: "2026-11-12T18:00:00Z",
    status: "ACTIVE",
    current: true,
  },
  {
    id: "s-other",
    userId: "u1",
    username: "zhang",
    ip: "10.0.0.9",
    userAgent: "Mozilla/5.0",
    amr: "pwd",
    createdAt: "2026-11-11T10:00:00Z",
    lastSeenAt: "2026-11-11T12:00:00Z",
    idleExpiresAt: "2026-11-11T13:00:00Z",
    absExpiresAt: "2026-11-11T18:00:00Z",
    status: "ACTIVE",
    current: false,
  },
];

describe("SessionsPage", () => {
  it("marks the current session and asks for confirmation before revoke", async () => {
    const user = userEvent.setup();
    const onRevoke = vi.fn().mockResolvedValue(undefined);
    render(<SessionsPage sessions={sessions} loading={false} error={null} onRevoke={onRevoke} onRevokeOthers={vi.fn()} />);
    expect(screen.getByText("当前会话")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "注销此会话" }));
    expect(onRevoke).not.toHaveBeenCalled();
    expect(screen.getByRole("dialog")).toHaveTextContent(/确认/);
    await user.click(screen.getByRole("button", { name: "确认注销" }));
    expect(onRevoke).toHaveBeenCalledWith("s-other");
  });
});

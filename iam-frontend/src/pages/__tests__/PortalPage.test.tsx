import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { PortalPage } from "../PortalPage";
import type { Application, Me } from "../../api/types";

const me: Me = {
  profile: {
    id: "u1",
    username: "zhang",
    displayName: "张三",
    email: "zhang@example.com",
    status: "ACTIVE",
  },
  mustChangePassword: false,
  admin: false,
  authorities: [],
};

const apps: Application[] = [
  {
    id: "a1",
    code: "employee-handbook",
    name: "员工手册",
    status: "ACTIVE",
    description: "查阅制度",
    launchUrl: "/examples/handbook",
    icon: "book",
    accentColor: "#3b5b7c",
  },
  {
    id: "a2",
    code: "hr-directory",
    name: "员工通讯录",
    status: "ACTIVE",
    description: "查找同事",
    launchUrl: "/examples/directory",
    icon: "users",
    accentColor: "#2a6f6a",
  },
];

describe("PortalPage", () => {
  it("greets the user and lists launchable applications", () => {
    render(
      <MemoryRouter>
        <PortalPage me={me} applications={apps} loading={false} error={null} />
      </MemoryRouter>,
    );
    expect(screen.getByText(/张三/)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /员工手册/ })).toHaveAttribute(
      "href",
      "/examples/handbook",
    );
    expect(screen.getByRole("link", { name: /员工手册/ })).toHaveAttribute("target", "_blank");
    expect(screen.queryByRole("link", { name: /管理控制台/ })).not.toBeInTheDocument();
  });

  it("filters applications and shows an empty state when nothing matches", async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter>
        <PortalPage me={me} applications={apps} loading={false} error={null} />
      </MemoryRouter>,
    );
    await user.type(screen.getByLabelText("搜索应用"), "不存在的系统");
    expect(screen.getByText(/没有找到匹配的应用/)).toBeInTheDocument();
  });

  it("shows a clear empty state when the user has no applications", () => {
    render(
      <MemoryRouter>
        <PortalPage me={me} applications={[]} loading={false} error={null} />
      </MemoryRouter>,
    );
    expect(screen.getByText(/暂时没有可打开的应用/)).toBeInTheDocument();
  });

  it("shows an admin console link for administrators", () => {
    render(
      <MemoryRouter>
        <PortalPage me={{ ...me, admin: true, authorities: ["ROLE_ADMIN"] }} applications={apps} loading={false} error={null} />
      </MemoryRouter>,
    );
    expect(screen.getByRole("link", { name: /管理控制台/ })).toHaveAttribute("href", "/admin");
  });
});

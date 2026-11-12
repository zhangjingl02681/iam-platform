import type { ApiError } from "./types";

const CSRF_COOKIE = "XSRF-TOKEN";
const CSRF_HEADER = "X-CSRF-TOKEN";
const REMEMBER_KEY = "iam.rememberedUsername";

export function rememberedUsername(): string | null {
  return localStorage.getItem(REMEMBER_KEY);
}

export function setRememberedUsername(username: string | null): void {
  if (username) {
    localStorage.setItem(REMEMBER_KEY, username);
  } else {
    localStorage.removeItem(REMEMBER_KEY);
  }
}

export function readCsrfToken(): string {
  const match = document.cookie.match(new RegExp(`(?:^|; )${CSRF_COOKIE}=([^;]*)`));
  return match ? decodeURIComponent(match[1]) : "";
}

export async function ensureCsrfToken(): Promise<string> {
  let token = readCsrfToken();
  if (token) {
    return token;
  }
  try {
    const response = await fetch("/csrf", { credentials: "include" });
    if (response.ok) {
      const body = (await response.json()) as { token?: string };
      token = body.token || readCsrfToken();
    }
  } catch {
    token = readCsrfToken();
  }
  return token;
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  const method = (init.method || "GET").toUpperCase();
  if (!headers.has("Accept")) {
    headers.set("Accept", "application/json");
  }
  const isFormData = typeof FormData !== "undefined" && init.body instanceof FormData;
  if (init.body && !isFormData && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  if (isFormData && headers.has("Content-Type")) {
    headers.delete("Content-Type");
  }
  if (method !== "GET" && method !== "HEAD") {
    const csrf = await ensureCsrfToken();
    if (csrf) {
      if (!headers.has(CSRF_HEADER)) {
        headers.set(CSRF_HEADER, csrf);
      }
      if (!headers.has("X-XSRF-TOKEN")) {
        headers.set("X-XSRF-TOKEN", csrf);
      }
    }
  }
  const response = await fetch(path, { ...init, method, headers, credentials: "include" });
  if (response.status === 401) {
    if (!window.location.pathname.startsWith("/login")) {
      window.location.href = "/login";
    }
    throw Object.assign(new Error("Unauthorized"), { status: 401, code: "unauthorized" }) as ApiError & Error;
  }
  if (response.status === 204) {
    return undefined as T;
  }
  const text = await response.text();
  const data = text ? (JSON.parse(text) as unknown) : undefined;
  if (!response.ok) {
    const problem = (data ?? {}) as { title?: string; detail?: string; code?: string };
    const error = Object.assign(new Error(problem.detail || problem.title || "请求失败"), {
      status: response.status,
      title: problem.title,
      detail: problem.detail,
      code: problem.code,
    }) as ApiError & Error;
    throw error;
  }
  return data as T;
}

export function formatDateTime(value?: string | null): string {
  if (!value) {
    return "—";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString("zh-CN", {
    timeZone: "Asia/Shanghai",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function statusLabel(status?: string | null): string {
  switch (status) {
    case "ACTIVE":
      return "启用";
    case "DISABLED":
      return "停用";
    case "TERMINATED":
      return "已终止";
    case "LOCKED":
      return "锁定";
    case "REVOKED":
      return "已注销";
    case "EXPIRED":
      return "已过期";
    case "PENDING":
      return "待处理";
    case "RUNNING":
      return "进行中";
    case "SUCCEEDED":
      return "已完成";
    case "FAILED":
      return "失败";
    case "CANCELLED":
      return "已取消";
    case "DRAFT":
      return "草稿";
    case "PUBLISHED":
      return "已发布";
    case "DEPRECATED":
      return "已停用";
    case "APPROVED":
      return "已批准";
    case "REJECTED":
      return "已拒绝";
    case "CHECKED_IN":
      return "已签入";
    case "CURRENT":
      return "当前";
    case "PREVIOUS":
      return "上一把";
    case "RETIRED":
      return "已退役";
    default:
      return status || "—";
  }
}

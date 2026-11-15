// The only place that knows how to talk to the backend: one base address, the CSRF header,
// and what to do when the session is no longer valid.

export type FieldError = { field: string; message: string };

export class ApiError extends Error {
  readonly status: number;
  readonly fieldErrors: FieldError[];

  constructor(status: number, message: string, fieldErrors: FieldError[] = []) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

/** The text shown when something fails and the server gave no message of its own. */
export const DEFAULT_ERROR_MESSAGE = "Something went wrong. Please try again.";

/** The message to show the user for a failed request: the server's own text, or the given fallback. */
export function errorMessage(error: unknown, fallback: string = DEFAULT_ERROR_MESSAGE): string {
  return error instanceof ApiError ? error.message : fallback;
}

const API_BASE = "/api/v1";
const CSRF_COOKIE = "XSRF-TOKEN";
const CSRF_HEADER = "X-XSRF-TOKEN";

let onUnauthorized: (() => void) | null = null;

/** Registers what happens when a request is refused with 401 (the session ended). */
export function setUnauthorizedHandler(handler: (() => void) | null): void {
  onUnauthorized = handler;
}

function readCookie(name: string): string | undefined {
  const entry = document.cookie.split("; ").find((cookie) => cookie.startsWith(`${name}=`));
  return entry ? decodeURIComponent(entry.slice(name.length + 1)) : undefined;
}

// The backend sets the token cookie on every response, even a 401. If it is missing (first
// visit), one harmless GET fetches it before the first request that changes data.
async function ensureCsrfToken(): Promise<string | undefined> {
  if (!readCookie(CSRF_COOKIE)) {
    await fetch(`${API_BASE}/users/me`, { credentials: "same-origin" }).catch(() => undefined);
  }
  return readCookie(CSRF_COOKIE);
}

type RequestOptions = {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  body?: unknown;
  /** Set to false for requests where a 401 is an expected answer (login, the first check of the session). */
  handleUnauthorized?: boolean;
};

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = "GET", body, handleUnauthorized = true } = options;
  const headers: Record<string, string> = { Accept: "application/json" };

  if (method !== "GET") {
    const token = await ensureCsrfToken();
    if (token) {
      headers[CSRF_HEADER] = token;
    }
    if (body !== undefined) {
      headers["Content-Type"] = "application/json";
    }
  }

  let response: Response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      credentials: "same-origin",
    });
  } catch {
    throw new ApiError(0, "Could not reach the server. Check your connection and try again.");
  }

  if (response.ok) {
    return (response.status === 204 ? undefined : await response.json()) as T;
  }

  const problem = (await response.json().catch(() => null)) as {
    detail?: string;
    errors?: FieldError[];
  } | null;

  if (response.status === 401 && handleUnauthorized) {
    onUnauthorized?.();
  }
  throw new ApiError(
    response.status,
    problem?.detail ?? DEFAULT_ERROR_MESSAGE,
    problem?.errors ?? [],
  );
}

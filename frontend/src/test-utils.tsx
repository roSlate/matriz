import { render } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { vi } from "vitest";
import type { Page, Project } from "./api/projects";
import type { Task } from "./api/tasks";
import App from "./App";
import { AuthProvider, type User } from "./auth/AuthContext";

export const ana: User = { id: 1, name: "Ana Silva", email: "ana@example.org" };

export function json(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": status >= 400 ? "application/problem+json" : "application/json" },
  });
}

export function problem(status: number, detail: string, extra: Record<string, unknown> = {}): Response {
  return json(status, { title: "Error", status, detail, ...extra });
}

export type Call = { method: string; path: string; body?: unknown };
type Handler = (call: Call) => Response | Promise<Response>;

/**
 * A stand-in for the backend. Routes are keyed "METHOD /path" (the query string is ignored when
 * matching); a request nobody handles gets a 404, so a forgotten route shows up in the test.
 */
export function mockApi(routes: Record<string, Handler>) {
  return vi.spyOn(globalThis, "fetch").mockImplementation(async (input, init) => {
    const method = init?.method ?? "GET";
    const path = String(input);
    const handler = routes[`${method} ${path}`] ?? routes[`${method} ${path.split("?")[0]}`];
    if (!handler) {
      return problem(404, `Not mocked: ${method} ${path}`);
    }
    return handler({ method, path, body: init?.body ? JSON.parse(String(init.body)) : undefined });
  });
}

export const signedInAsAna = { "GET /api/v1/users/me": () => json(200, ana) };

export function requests(spy: ReturnType<typeof mockApi>, method: string, pathPart: string) {
  return spy.mock.calls.filter(
    ([url, init]) => (init?.method ?? "GET") === method && String(url).includes(pathPart),
  );
}

export function renderApp(path = "/") {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AuthProvider>
        <App />
      </AuthProvider>
    </MemoryRouter>,
  );
}

export function project(overrides: Partial<Project> = {}): Project {
  return {
    id: 1,
    name: "Website redesign",
    description: "New landing page and style guide.",
    myRole: "OWNER",
    taskCounts: { todo: 4, inProgress: 2, done: 6, total: 12 },
    progress: { computed: 50, manual: null, effective: 50 },
    createdAt: "2026-11-14T10:00:00Z",
    updatedAt: "2026-11-14T10:00:00Z",
    ...overrides,
  };
}

export function pageOf(items: Project[], overrides: Partial<Page<Project>> = {}): Page<Project> {
  return { items, page: 0, size: 12, totalItems: items.length, totalPages: items.length === 0 ? 0 : 1, ...overrides };
}

export function task(overrides: Partial<Task> = {}): Task {
  return {
    id: 5,
    projectId: 1,
    title: "Write the project brief",
    description: "One page, for the client.",
    status: "TODO",
    priority: "MEDIUM",
    assignees: [],
    createdAt: "2026-11-14T10:00:00Z",
    updatedAt: "2026-11-15T09:00:00Z",
    ...overrides,
  };
}

export function taskPageOf(items: Task[], overrides: Partial<Page<Task>> = {}): Page<Task> {
  return { items, page: 0, size: 10, totalItems: items.length, totalPages: items.length === 0 ? 0 : 1, ...overrides };
}

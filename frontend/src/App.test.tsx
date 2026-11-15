import { act, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "./App";
import { apiRequest } from "./api/client";
import { AuthProvider, type User } from "./auth/AuthContext";

const ana: User = { id: 1, name: "Ana Silva", email: "ana@example.org" };

function json(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": status >= 400 ? "application/problem+json" : "application/json" },
  });
}

const problem = (status: number, detail: string) => json(status, { title: "Error", status, detail });

// A tiny stand-in for the backend: it knows who is signed in and answers the auth routes.
function fakeBackend(options: { signedIn?: User | null; loginFails?: boolean } = {}) {
  let signedIn = options.signedIn ?? null;
  return vi.spyOn(globalThis, "fetch").mockImplementation(async (input, init) => {
    const method = init?.method ?? "GET";
    const path = String(input);
    if (path === "/api/v1/users/me") {
      return signedIn ? json(200, signedIn) : problem(401, "Authentication is required.");
    }
    if (path === "/api/v1/auth/login" && method === "POST") {
      if (options.loginFails) {
        return problem(401, "Invalid email or password.");
      }
      signedIn = ana;
      return json(200, ana);
    }
    if (path === "/api/v1/auth/logout" && method === "POST") {
      signedIn = null;
      return new Response(null, { status: 204 });
    }
    if (path === "/api/v1/projects") {
      return problem(401, "Authentication is required.");
    }
    return problem(404, "Not found.");
  });
}

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AuthProvider>
        <App />
      </AuthProvider>
    </MemoryRouter>,
  );
}

const loginCalls = (spy: ReturnType<typeof fakeBackend>) =>
  spy.mock.calls.filter(([url, init]) => String(url).endsWith("/auth/login") && init?.method === "POST");

describe("authentication screens", () => {
  afterEach(() => vi.restoreAllMocks());

  it("sends a visitor without a session to the login page", async () => {
    fakeBackend({ signedIn: null });

    renderAt("/");

    expect(await screen.findByRole("heading", { name: "Log in" })).toBeInTheDocument();
  });

  it("asks for email and password before contacting the server", async () => {
    const backend = fakeBackend();
    renderAt("/login");
    const user = userEvent.setup();

    await user.click(await screen.findByRole("button", { name: "Log in" }));

    expect(screen.getByText("Email is required.")).toBeInTheDocument();
    expect(screen.getByText("Password is required.")).toBeInTheDocument();
    expect(loginCalls(backend)).toHaveLength(0);
  });

  it("treats a password of only spaces as empty", async () => {
    const backend = fakeBackend();
    renderAt("/login");
    const user = userEvent.setup();

    await user.type(await screen.findByLabelText("Email"), "ana@example.org");
    await user.type(screen.getByLabelText("Password"), "    ");
    await user.click(screen.getByRole("button", { name: "Log in" }));

    expect(screen.getByText("Password is required.")).toBeInTheDocument();
    expect(loginCalls(backend)).toHaveLength(0);
  });

  it("shows the server's message when the credentials are wrong and keeps the email", async () => {
    fakeBackend({ loginFails: true });
    renderAt("/login");
    const user = userEvent.setup();

    await user.type(await screen.findByLabelText("Email"), "ana@example.org");
    await user.type(screen.getByLabelText("Password"), "wrong-password");
    await user.click(screen.getByRole("button", { name: "Log in" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Invalid email or password.");
    expect(screen.getByLabelText("Email")).toHaveValue("ana@example.org");
  });

  it("shows the user's name after a successful login", async () => {
    fakeBackend();
    renderAt("/login");
    const user = userEvent.setup();

    await user.type(await screen.findByLabelText("Email"), "ana@example.org");
    await user.type(screen.getByLabelText("Password"), "correct-password");
    await user.click(screen.getByRole("button", { name: "Log in" }));

    expect(await screen.findByText("Ana Silva")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "My projects" })).toBeInTheDocument();
  });

  it("logs out and returns to the login page", async () => {
    fakeBackend({ signedIn: ana });
    renderAt("/");
    const user = userEvent.setup();

    await user.click(await screen.findByRole("button", { name: "Log out" }));

    expect(await screen.findByRole("heading", { name: "Log in" })).toBeInTheDocument();
  });

  it("tells the user the session expired when the server later answers 401", async () => {
    fakeBackend({ signedIn: ana });
    renderAt("/");
    await screen.findByText("Ana Silva");

    await act(async () => {
      await apiRequest("/projects").catch(() => undefined);
    });

    expect(await screen.findByRole("heading", { name: "Log in" })).toBeInTheDocument();
    expect(screen.getByText("Your session has expired. Please log in again.")).toBeInTheDocument();
  });

  it("does not claim the session expired for a visitor who was never signed in", async () => {
    fakeBackend({ signedIn: null });

    renderAt("/");

    expect(await screen.findByRole("heading", { name: "Log in" })).toBeInTheDocument();
    expect(screen.queryByText("Your session has expired. Please log in again.")).not.toBeInTheDocument();
  });
});

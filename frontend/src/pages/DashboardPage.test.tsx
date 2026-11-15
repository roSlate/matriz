import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import type { Project, ProjectInput } from "../api/projects";
import { json, mockApi, pageOf, problem, project, renderApp, requests, signedInAsAna } from "../test-utils";

describe("dashboard", () => {
  afterEach(() => vi.restoreAllMocks());

  it("lists the user's projects with role, task counts and progress", async () => {
    mockApi({
      ...signedInAsAna,
      "GET /api/v1/projects": () =>
        json(
          200,
          pageOf([
            project(),
            project({
              id: 2,
              name: "Client onboarding",
              myRole: "MEMBER",
              taskCounts: { todo: 3, inProgress: 1, done: 2, total: 6 },
              progress: { computed: 33, manual: 80, effective: 80 },
            }),
          ]),
        ),
    });

    renderApp("/");

    const first = await screen.findByRole("link", { name: /Website redesign/ });
    expect(within(first).getByText("Owner")).toBeInTheDocument();
    expect(within(first).getByRole("list", { name: "Tasks per status" })).toHaveTextContent(/To do\s*4/);
    expect(within(first).getByText("Progress: 50%")).toBeInTheDocument();

    const second = screen.getByRole("link", { name: /Client onboarding/ });
    expect(within(second).getByText("Member")).toBeInTheDocument();
    expect(within(second).getByText("Progress: 80% (manual)")).toBeInTheDocument();
    expect(within(second).getByText("Computed from tasks: 33%")).toBeInTheDocument();
    expect(screen.getByText("Showing 2 of 2 projects")).toBeInTheDocument();
  });

  it("links each card to its project", async () => {
    mockApi({ ...signedInAsAna, "GET /api/v1/projects": () => json(200, pageOf([project({ id: 7 })])) });

    renderApp("/");

    expect(await screen.findByRole("link", { name: /Website redesign/ })).toHaveAttribute("href", "/projects/7");
  });

  it("shows an empty state with a create action when there are no projects", async () => {
    mockApi({ ...signedInAsAna, "GET /api/v1/projects": () => json(200, pageOf([])) });

    renderApp("/");

    expect(await screen.findByText("You have no projects yet.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Create your first project" })).toBeInTheDocument();
  });

  it("shows a plain error and lets the user try again", async () => {
    let calls = 0;
    mockApi({
      ...signedInAsAna,
      "GET /api/v1/projects": () => (++calls === 1 ? problem(500, "boom") : json(200, pageOf([project()]))),
    });
    renderApp("/");
    const user = userEvent.setup();

    expect(await screen.findByRole("alert")).toHaveTextContent("Could not load your projects.");
    expect(screen.queryByText("boom")).not.toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Try again" }));

    expect(await screen.findByRole("link", { name: /Website redesign/ })).toBeInTheDocument();
  });

  it("moves between pages", async () => {
    mockApi({
      ...signedInAsAna,
      "GET /api/v1/projects": ({ path }) =>
        path.includes("page=1")
          ? json(200, pageOf([project({ id: 13, name: "Second page project" })], { page: 1, totalItems: 13, totalPages: 2 }))
          : json(200, pageOf([project()], { totalItems: 13, totalPages: 2 })),
    });
    renderApp("/");
    const user = userEvent.setup();

    expect(await screen.findByText("Page 1 of 2")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Previous" })).toBeDisabled();
    await user.click(screen.getByRole("button", { name: "Next" }));

    expect(await screen.findByRole("link", { name: /Second page project/ })).toBeInTheDocument();
    expect(screen.getByText("Page 2 of 2")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Next" })).toBeDisabled();
  });
});

describe("creating a project", () => {
  afterEach(() => vi.restoreAllMocks());

  async function openForm(user: ReturnType<typeof userEvent.setup>) {
    await user.click(await screen.findByRole("button", { name: "New project" }));
    return screen.getByRole("dialog", { name: "New project" });
  }

  it("checks the name before contacting the server and keeps what was typed", async () => {
    const api = mockApi({
      ...signedInAsAna,
      "GET /api/v1/projects": () => json(200, pageOf([])),
      "POST /api/v1/projects": () => json(201, project({ id: 9, name: "Launch plan" })),
    });
    renderApp("/");
    const user = userEvent.setup();
    const dialog = await openForm(user);

    await user.type(within(dialog).getByLabelText("Name"), "  ab  ");
    await user.click(within(dialog).getByRole("button", { name: "Create project" }));

    expect(within(dialog).getByText("Must be between 3 and 100 characters.")).toBeInTheDocument();
    expect(within(dialog).getByLabelText("Name")).toHaveValue("  ab  ");
    expect(requests(api, "POST", "/projects")).toHaveLength(0);
  });

  it("refuses an empty name", async () => {
    const api = mockApi({ ...signedInAsAna, "GET /api/v1/projects": () => json(200, pageOf([])) });
    renderApp("/");
    const user = userEvent.setup();
    const dialog = await openForm(user);

    await user.click(within(dialog).getByRole("button", { name: "Create project" }));

    expect(within(dialog).getByText("Must not be empty.")).toBeInTheDocument();
    expect(requests(api, "POST", "/projects")).toHaveLength(0);
  });

  it("creates the project, closes the form and shows the new project once", async () => {
    const stored: Project[] = [];
    const api = mockApi({
      ...signedInAsAna,
      "GET /api/v1/projects": () => json(200, pageOf([...stored])),
      "POST /api/v1/projects": ({ body }) => {
        const created = project({ id: 9, name: (body as ProjectInput).name });
        stored.unshift(created);
        return json(201, created);
      },
    });
    renderApp("/");
    const user = userEvent.setup();
    const dialog = await openForm(user);

    await user.type(within(dialog).getByLabelText("Name"), "  Launch plan  ");
    await user.click(within(dialog).getByRole("button", { name: "Create project" }));

    expect(await screen.findByRole("link", { name: /Launch plan/ })).toBeInTheDocument();
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(screen.getAllByRole("link", { name: /Launch plan/ })).toHaveLength(1);
    expect(requests(api, "POST", "/projects")[0][1]?.body).toBe(JSON.stringify({ name: "Launch plan", description: "" }));
  });

  it("shows the server's field error under the field and keeps the input", async () => {
    mockApi({
      ...signedInAsAna,
      "GET /api/v1/projects": () => json(200, pageOf([])),
      "POST /api/v1/projects": () =>
        problem(400, "Validation failed.", { errors: [{ field: "name", message: "Must be between 3 and 100 characters." }] }),
    });
    renderApp("/");
    const user = userEvent.setup();
    const dialog = await openForm(user);

    await user.type(within(dialog).getByLabelText("Name"), "Launch plan");
    await user.click(within(dialog).getByRole("button", { name: "Create project" }));

    expect(await within(dialog).findByText("Must be between 3 and 100 characters.")).toBeInTheDocument();
    expect(within(dialog).getByLabelText("Name")).toHaveValue("Launch plan");
    expect(screen.getByRole("dialog", { name: "New project" })).toBeInTheDocument();
  });

  it("closes without saving when the user cancels", async () => {
    const api = mockApi({ ...signedInAsAna, "GET /api/v1/projects": () => json(200, pageOf([])) });
    renderApp("/");
    const user = userEvent.setup();
    const dialog = await openForm(user);

    await user.click(within(dialog).getByRole("button", { name: "Cancel" }));

    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(requests(api, "POST", "/projects")).toHaveLength(0);
  });
});

describe("searching projects", () => {
  afterEach(() => vi.restoreAllMocks());

  const searchOf = (path: string) => new URL(path, "http://localhost").searchParams.get("search");

  it("waits for a pause in typing, then shows only the matching projects", async () => {
    const api = mockApi({
      ...signedInAsAna,
      "GET /api/v1/projects": ({ path }) =>
        json(
          200,
          searchOf(path) === "redesign"
            ? pageOf([project({ id: 1, name: "Website redesign" })])
            : pageOf([project({ id: 1, name: "Website redesign" }), project({ id: 2, name: "Client onboarding" })]),
        ),
    });
    renderApp("/");
    const user = userEvent.setup();
    expect(await screen.findByRole("link", { name: /Client onboarding/ })).toBeInTheDocument();

    await user.type(screen.getByLabelText("Search projects"), "redesign");

    await vi.waitFor(() => expect(screen.queryByRole("link", { name: /Client onboarding/ })).not.toBeInTheDocument());
    expect(screen.getByRole("link", { name: /Website redesign/ })).toBeInTheDocument();
    const searches = requests(api, "GET", "/projects").map(([url]) => searchOf(String(url)));
    expect(searches.at(-1)).toBe("redesign");
    // One request after the pause, not one per keystroke.
    expect(searches.filter((value) => value !== null)).toHaveLength(1);
  });

  it("says no project matches, which is different from having none, and clears the search", async () => {
    mockApi({
      ...signedInAsAna,
      "GET /api/v1/projects": ({ path }) =>
        json(200, searchOf(path) ? pageOf([]) : pageOf([project({ id: 1, name: "Website redesign" })])),
    });
    renderApp("/");
    const user = userEvent.setup();
    await screen.findByRole("link", { name: /Website redesign/ });

    await user.type(screen.getByLabelText("Search projects"), "zzz");

    expect(await screen.findByText("No projects match your search.")).toBeInTheDocument();
    expect(screen.queryByText("You have no projects yet.")).not.toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Clear search" }));

    expect(await screen.findByRole("link", { name: /Website redesign/ })).toBeInTheDocument();
    expect(screen.getByLabelText("Search projects")).toHaveValue("");
  });

  it("goes back to the first page when the search changes", async () => {
    const api = mockApi({
      ...signedInAsAna,
      "GET /api/v1/projects": ({ path }) =>
        json(200, pageOf([project()], { totalItems: 13, totalPages: path.includes("search=") ? 1 : 2 })),
    });
    renderApp("/");
    const user = userEvent.setup();
    await user.click(await screen.findByRole("button", { name: "Next" }));
    await screen.findByText("Page 2 of 2");

    await user.type(screen.getByLabelText("Search projects"), "web");

    await vi.waitFor(() => {
      const last = String(requests(api, "GET", "/projects").at(-1)?.[0]);
      expect(last).toContain("search=web");
      expect(last).toContain("page=0");
    });
  });
});

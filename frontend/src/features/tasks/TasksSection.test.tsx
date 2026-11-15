import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import type { Task, TaskInput } from "../../api/tasks";
import {
  json,
  mockApi,
  problem,
  project,
  renderApp,
  requests,
  signedInAsAna,
  task,
  taskPageOf,
} from "../../test-utils";

const projectAs = (role: "OWNER" | "MEMBER" | "VIEWER") => ({
  ...signedInAsAna,
  "GET /api/v1/projects/1": () => json(200, project({ myRole: role })),
  // The page also shows the members; an empty list keeps these task tests about tasks only.
  "GET /api/v1/projects/1/members": () => json(200, []),
});

const ana = { id: 1, name: "Ana Silva", email: "ana@example.org" };
const rui = { id: 2, name: "Rui Costa", email: "rui@example.org" };

describe("tasks on the project page", () => {
  afterEach(() => vi.restoreAllMocks());

  it("lists the tasks with status, priority and assignees, and links each to its page", async () => {
    mockApi({
      ...projectAs("OWNER"),
      "GET /api/v1/projects/1/tasks": () =>
        json(
          200,
          taskPageOf([
            task({ id: 5, title: "Write the project brief", status: "IN_PROGRESS", priority: "HIGH", assignees: [ana, rui] }),
            task({ id: 6, title: "Design the landing page" }),
          ]),
        ),
    });

    renderApp("/projects/1");

    const table = await screen.findByRole("table", { name: "Tasks" });
    expect(within(table).getByRole("link", { name: "Write the project brief" })).toHaveAttribute("href", "/tasks/5");
    const first = within(table).getByRole("row", { name: /Write the project brief/ });
    expect(first).toHaveTextContent("In progress");
    expect(first).toHaveTextContent("High");
    expect(first).toHaveTextContent("Ana Silva, Rui Costa");
    expect(within(table).getByRole("row", { name: /Design the landing page/ })).toHaveTextContent("Unassigned");
    expect(screen.getByText("Showing 2 of 2 tasks")).toBeInTheDocument();
  });

  it("filters by status and priority, combines them, and clears them", async () => {
    const api = mockApi({
      ...projectAs("OWNER"),
      "GET /api/v1/projects/1/tasks": () => json(200, taskPageOf([task()])),
    });
    renderApp("/projects/1");
    const user = userEvent.setup();
    await screen.findByRole("table", { name: "Tasks" });
    const lastTaskRequest = () => String(requests(api, "GET", "/projects/1/tasks").at(-1)?.[0]);

    await user.selectOptions(screen.getByLabelText("Status"), "DONE");
    await screen.findByRole("table", { name: "Tasks" });
    expect(lastTaskRequest()).toContain("status=DONE");
    expect(lastTaskRequest()).not.toContain("priority=");

    await user.selectOptions(screen.getByLabelText("Priority"), "HIGH");
    await screen.findByRole("table", { name: "Tasks" });
    expect(lastTaskRequest()).toContain("status=DONE");
    expect(lastTaskRequest()).toContain("priority=HIGH");

    await user.click(screen.getByRole("button", { name: "Clear filters" }));
    await screen.findByRole("table", { name: "Tasks" });
    expect(lastTaskRequest()).not.toContain("status=");
    expect(lastTaskRequest()).not.toContain("priority=");
    expect(screen.getByLabelText("Status")).toHaveValue("");
  });

  it("says no task matches, which is not an error, and offers to clear the filters", async () => {
    mockApi({
      ...projectAs("OWNER"),
      "GET /api/v1/projects/1/tasks": ({ path }) =>
        json(200, path.includes("status=DONE") ? taskPageOf([]) : taskPageOf([task()])),
    });
    renderApp("/projects/1");
    const user = userEvent.setup();
    await screen.findByRole("table", { name: "Tasks" });

    await user.selectOptions(screen.getByLabelText("Status"), "DONE");

    expect(await screen.findByText("No tasks match the filters.")).toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    await user.click(screen.getAllByRole("button", { name: "Clear filters" })[0]);
    expect(await screen.findByRole("table", { name: "Tasks" })).toBeInTheDocument();
  });

  it.each(["OWNER", "MEMBER"] as const)("offers to create the first task to a %s", async (role) => {
    mockApi({ ...projectAs(role), "GET /api/v1/projects/1/tasks": () => json(200, taskPageOf([])) });

    renderApp("/projects/1");

    expect(await screen.findByText("No tasks yet.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Create the first task" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "New task" })).toBeInTheDocument();
  });

  it("lets a Viewer read the tasks but offers no way to create one", async () => {
    mockApi({
      ...projectAs("VIEWER"),
      "GET /api/v1/projects/1/tasks": () => json(200, taskPageOf([task()])),
    });

    renderApp("/projects/1");

    expect(await screen.findByRole("table", { name: "Tasks" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "New task" })).not.toBeInTheDocument();
  });

  it("tells a Viewer there are no tasks without offering to create one", async () => {
    mockApi({ ...projectAs("VIEWER"), "GET /api/v1/projects/1/tasks": () => json(200, taskPageOf([])) });

    renderApp("/projects/1");

    expect(await screen.findByText("No tasks yet.")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Create the first task" })).not.toBeInTheDocument();
  });

  it("shows a plain error and lets the user try again", async () => {
    let calls = 0;
    mockApi({
      ...projectAs("OWNER"),
      "GET /api/v1/projects/1/tasks": () => (++calls === 1 ? problem(500, "boom") : json(200, taskPageOf([task()]))),
    });
    renderApp("/projects/1");
    const user = userEvent.setup();

    expect(await screen.findByRole("alert")).toHaveTextContent("Could not load the tasks.");
    await user.click(screen.getByRole("button", { name: "Try again" }));

    expect(await screen.findByRole("table", { name: "Tasks" })).toBeInTheDocument();
  });

  it("moves between pages", async () => {
    mockApi({
      ...projectAs("OWNER"),
      "GET /api/v1/projects/1/tasks": ({ path }) =>
        path.includes("page=1")
          ? json(200, taskPageOf([task({ id: 11, title: "Second page task" })], { page: 1, totalItems: 11, totalPages: 2 }))
          : json(200, taskPageOf([task()], { totalItems: 11, totalPages: 2 })),
    });
    renderApp("/projects/1");
    const user = userEvent.setup();

    expect(await screen.findByText("Page 1 of 2")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Next" }));

    expect(await screen.findByRole("link", { name: "Second page task" })).toBeInTheDocument();
    expect(screen.getByText("Page 2 of 2")).toBeInTheDocument();
  });
});

describe("creating a task", () => {
  afterEach(() => vi.restoreAllMocks());

  async function openForm(user: ReturnType<typeof userEvent.setup>) {
    await user.click(await screen.findByRole("button", { name: "New task" }));
    return screen.getByRole("dialog", { name: "New task" });
  }

  it("checks the title before contacting the server and keeps what was typed", async () => {
    const api = mockApi({
      ...projectAs("MEMBER"),
      "GET /api/v1/projects/1/tasks": () => json(200, taskPageOf([])),
      "POST /api/v1/projects/1/tasks": () => json(201, task()),
    });
    renderApp("/projects/1");
    const user = userEvent.setup();
    const dialog = await openForm(user);

    await user.type(within(dialog).getByLabelText("Title"), "  ab  ");
    await user.click(within(dialog).getByRole("button", { name: "Create task" }));

    expect(within(dialog).getByText("Must be between 3 and 150 characters.")).toBeInTheDocument();
    expect(within(dialog).getByLabelText("Title")).toHaveValue("  ab  ");
    expect(requests(api, "POST", "/projects/1/tasks")).toHaveLength(0);
  });

  it("refuses a title of only spaces", async () => {
    const api = mockApi({ ...projectAs("MEMBER"), "GET /api/v1/projects/1/tasks": () => json(200, taskPageOf([])) });
    renderApp("/projects/1");
    const user = userEvent.setup();
    const dialog = await openForm(user);

    await user.type(within(dialog).getByLabelText("Title"), "     ");
    await user.click(within(dialog).getByRole("button", { name: "Create task" }));

    expect(within(dialog).getByText("Must not be empty.")).toBeInTheDocument();
    expect(requests(api, "POST", "/projects/1/tasks")).toHaveLength(0);
  });

  it("creates the task with the chosen values, shows it once, and refreshes the project's counts", async () => {
    const stored: Task[] = [];
    const api = mockApi({
      ...projectAs("MEMBER"),
      "GET /api/v1/projects/1/tasks": () => json(200, taskPageOf([...stored])),
      "POST /api/v1/projects/1/tasks": ({ body }) => {
        const input = body as TaskInput;
        const created = task({ id: 9, title: input.title, status: input.status, priority: input.priority });
        stored.unshift(created);
        return json(201, created);
      },
    });
    renderApp("/projects/1");
    const user = userEvent.setup();
    const dialog = await openForm(user);

    expect(within(dialog).getByLabelText("Status")).toHaveValue("TODO");
    expect(within(dialog).getByLabelText("Priority")).toHaveValue("MEDIUM");
    await user.type(within(dialog).getByLabelText("Title"), "  Plan the launch  ");
    await user.selectOptions(within(dialog).getByLabelText("Priority"), "HIGH");
    await user.click(within(dialog).getByRole("button", { name: "Create task" }));

    expect(await screen.findByRole("link", { name: "Plan the launch" })).toBeInTheDocument();
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(screen.getAllByRole("link", { name: "Plan the launch" })).toHaveLength(1);
    expect(requests(api, "POST", "/projects/1/tasks")[0][1]?.body).toBe(
      JSON.stringify({ title: "Plan the launch", description: "", status: "TODO", priority: "HIGH" }),
    );
    // The project was loaded once on arrival and once more after the task was created.
    const projectLoads = api.mock.calls.filter(([url]) => String(url) === "/api/v1/projects/1");
    expect(projectLoads).toHaveLength(2);
  });

  it("shows the server's field error under the field and keeps the input", async () => {
    mockApi({
      ...projectAs("MEMBER"),
      "GET /api/v1/projects/1/tasks": () => json(200, taskPageOf([])),
      "POST /api/v1/projects/1/tasks": () =>
        problem(400, "Validation failed.", { errors: [{ field: "title", message: "Must be between 3 and 150 characters." }] }),
    });
    renderApp("/projects/1");
    const user = userEvent.setup();
    const dialog = await openForm(user);

    await user.type(within(dialog).getByLabelText("Title"), "Plan the launch");
    await user.click(within(dialog).getByRole("button", { name: "Create task" }));

    expect(await within(dialog).findByText("Must be between 3 and 150 characters.")).toBeInTheDocument();
    expect(within(dialog).getByLabelText("Title")).toHaveValue("Plan the launch");
    expect(screen.getByRole("dialog", { name: "New task" })).toBeInTheDocument();
  });

  it("keeps the form open and says so when the server cannot be reached", async () => {
    mockApi({
      ...projectAs("MEMBER"),
      "GET /api/v1/projects/1/tasks": () => json(200, taskPageOf([])),
      "POST /api/v1/projects/1/tasks": () => problem(500, "Something went wrong. Please try again."),
    });
    renderApp("/projects/1");
    const user = userEvent.setup();
    const dialog = await openForm(user);

    await user.type(within(dialog).getByLabelText("Title"), "Plan the launch");
    await user.click(within(dialog).getByRole("button", { name: "Create task" }));

    expect(await within(dialog).findByRole("alert")).toHaveTextContent("Something went wrong. Please try again.");
    expect(within(dialog).getByLabelText("Title")).toHaveValue("Plan the launch");
  });
});

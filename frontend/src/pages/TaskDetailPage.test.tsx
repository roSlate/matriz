import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { json, mockApi, problem, project, renderApp, requests, signedInAsAna, task, taskPageOf } from "../test-utils";

const ana = { id: 1, name: "Ana Silva", email: "ana@example.org" };
const rui = { id: 2, name: "Rui Costa", email: "rui@example.org" };

const taskRoutes = (role: "OWNER" | "MEMBER" | "VIEWER" = "OWNER", overrides = {}) => ({
  ...signedInAsAna,
  "GET /api/v1/tasks/5": () => json(200, task(overrides)),
  "GET /api/v1/projects/1": () => json(200, project({ myRole: role })),
  // The people who can be assigned; the assignee tests have their own file.
  "GET /api/v1/projects/1/members": () => json(200, []),
  "GET /api/v1/tasks/5/comments": () => json(200, { items: [], page: 0, size: 20, totalItems: 0, totalPages: 0 }),
});

describe("task page", () => {
  afterEach(() => vi.restoreAllMocks());

  it("shows the task, its project in the breadcrumb, and its assignees", async () => {
    mockApi(taskRoutes("OWNER", { status: "IN_PROGRESS", priority: "HIGH", assignees: [ana, rui] }));

    renderApp("/tasks/5");

    expect(await screen.findByRole("heading", { level: 1, name: "Write the project brief" })).toBeInTheDocument();
    expect(screen.getByText("One page, for the client.")).toBeInTheDocument();
    expect(screen.getByLabelText("Status")).toHaveValue("IN_PROGRESS");
    expect(screen.getByLabelText("Priority")).toHaveValue("HIGH");
    expect(within(screen.getByRole("list", { name: "Assignees" })).getByText("Rui Costa")).toBeInTheDocument();
    const breadcrumb = screen.getByRole("navigation", { name: "Breadcrumb" });
    expect(within(breadcrumb).getByRole("link", { name: "Website redesign" })).toHaveAttribute("href", "/projects/1");
  });

  it("says Unassigned for a task without assignees", async () => {
    mockApi(taskRoutes());

    renderApp("/tasks/5");

    expect(await screen.findByText("Unassigned")).toBeInTheDocument();
  });

  it("changes the status from the list and shows what the server confirmed", async () => {
    const api = mockApi({
      ...taskRoutes("MEMBER"),
      "PATCH /api/v1/tasks/5": ({ body }) => json(200, task({ status: (body as { status: "DONE" }).status })),
    });
    renderApp("/tasks/5");
    const user = userEvent.setup();

    await user.selectOptions(await screen.findByLabelText("Status"), "DONE");

    expect(await screen.findByDisplayValue("Done")).toBeInTheDocument();
    expect(requests(api, "PATCH", "/tasks/5")[0][1]?.body).toBe(JSON.stringify({ status: "DONE" }));
  });

  it("can reopen a done task", async () => {
    mockApi({
      ...taskRoutes("OWNER", { status: "DONE" }),
      "PATCH /api/v1/tasks/5": () => json(200, task({ status: "TODO" })),
    });
    renderApp("/tasks/5");
    const user = userEvent.setup();

    await user.selectOptions(await screen.findByLabelText("Status"), "TODO");

    expect(await screen.findByDisplayValue("To do")).toBeInTheDocument();
  });

  it("leaves the status as it was and shows the error when saving fails", async () => {
    mockApi({
      ...taskRoutes("OWNER", { status: "TODO" }),
      "PATCH /api/v1/tasks/5": () => problem(500, "Something went wrong. Please try again."),
    });
    renderApp("/tasks/5");
    const user = userEvent.setup();

    await user.selectOptions(await screen.findByLabelText("Status"), "DONE");

    expect(await screen.findByRole("alert")).toHaveTextContent("Something went wrong. Please try again.");
    expect(screen.getByLabelText("Status")).toHaveValue("TODO");
  });

  it("shows a Viewer the values as text, with no controls to change them", async () => {
    mockApi(taskRoutes("VIEWER", { status: "IN_PROGRESS", priority: "HIGH" }));

    renderApp("/tasks/5");

    expect(await screen.findByText("In progress")).toBeInTheDocument();
    expect(screen.getByText("High")).toBeInTheDocument();
    expect(screen.queryByLabelText("Status")).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Edit" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Delete task" })).not.toBeInTheDocument();
  });

  it("edits the title and description and shows what the server confirmed", async () => {
    const api = mockApi({
      ...taskRoutes("OWNER"),
      "PATCH /api/v1/tasks/5": ({ body }) => json(200, task({ title: (body as { title: string }).title })),
    });
    renderApp("/tasks/5");
    const user = userEvent.setup();

    await user.click(await screen.findByRole("button", { name: "Edit" }));
    const dialog = screen.getByRole("dialog", { name: "Edit task" });
    expect(within(dialog).getByLabelText("Title")).toHaveValue("Write the project brief");
    await user.clear(within(dialog).getByLabelText("Title"));
    await user.type(within(dialog).getByLabelText("Title"), "Write the full brief");
    await user.click(within(dialog).getByRole("button", { name: "Save changes" }));

    expect(await screen.findByRole("heading", { level: 1, name: "Write the full brief" })).toBeInTheDocument();
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(requests(api, "PATCH", "/tasks/5")[0][1]?.body).toBe(
      JSON.stringify({
        title: "Write the full brief",
        description: "One page, for the client.",
        status: "TODO",
        priority: "MEDIUM",
      }),
    );
  });

  it("does not delete anything when the user cancels the confirmation", async () => {
    const api = mockApi(taskRoutes("OWNER"));
    renderApp("/tasks/5");
    const user = userEvent.setup();

    await user.click(await screen.findByRole("button", { name: "Delete task" }));
    const dialog = screen.getByRole("dialog", { name: 'Delete "Write the project brief"?' });
    expect(dialog).toHaveTextContent("permanently deleted");
    await user.click(within(dialog).getByRole("button", { name: "Cancel" }));

    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(requests(api, "DELETE", "/tasks/5")).toHaveLength(0);
    expect(screen.getByRole("heading", { level: 1, name: "Write the project brief" })).toBeInTheDocument();
  });

  it("deletes the task after confirmation and goes back to the project", async () => {
    const api = mockApi({
      ...taskRoutes("OWNER"),
      "DELETE /api/v1/tasks/5": () => new Response(null, { status: 204 }),
      "GET /api/v1/projects/1/tasks": () => json(200, taskPageOf([])),
    });
    renderApp("/tasks/5");
    const user = userEvent.setup();

    await user.click(await screen.findByRole("button", { name: "Delete task" }));
    const dialog = screen.getByRole("dialog");
    await user.click(within(dialog).getByRole("button", { name: "Delete task" }));

    expect(await screen.findByRole("heading", { level: 1, name: "Website redesign" })).toBeInTheDocument();
    expect(requests(api, "DELETE", "/tasks/5")).toHaveLength(1);
  });

  it("keeps the dialog open and shows the error when deleting fails, never a success", async () => {
    mockApi({
      ...taskRoutes("OWNER"),
      "DELETE /api/v1/tasks/5": () => problem(500, "Something went wrong. Please try again."),
    });
    renderApp("/tasks/5");
    const user = userEvent.setup();

    await user.click(await screen.findByRole("button", { name: "Delete task" }));
    const dialog = screen.getByRole("dialog");
    await user.click(within(dialog).getByRole("button", { name: "Delete task" }));

    expect(await within(dialog).findByRole("alert")).toHaveTextContent("Something went wrong. Please try again.");
    expect(screen.getByRole("dialog")).toBeInTheDocument();
    expect(screen.queryByRole("heading", { level: 1, name: "Website redesign" })).not.toBeInTheDocument();
  });

  it("explains that the task is gone, without saying whether it ever existed", async () => {
    mockApi({ ...signedInAsAna, "GET /api/v1/tasks/99": () => problem(404, "Resource not found.") });

    renderApp("/tasks/99");

    expect(await screen.findByText("This task no longer exists, or you no longer have access to it.")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Back to my projects" })).toHaveAttribute("href", "/");
  });
});

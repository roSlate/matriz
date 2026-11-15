import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import type { Member } from "../../api/members";
import { json, mockApi, problem, project, renderApp, requests, signedInAsAna, task } from "../../test-utils";

const ana = { id: 1, name: "Ana Silva", email: "ana@example.org" };
const rui = { id: 2, name: "Rui Costa", email: "rui@example.org" };

const member = (person: typeof ana, role: Member["role"]): Member => ({
  userId: person.id,
  ...person,
  role,
  addedAt: "2026-11-01T10:00:00Z",
});
const SOFIA = { id: 3, name: "Sofia Lima", email: "sofia@example.org" };
const MEMBERS = [member(ana, "OWNER"), member(rui, "MEMBER"), member(SOFIA, "VIEWER")];

const routes = (role: "OWNER" | "MEMBER" | "VIEWER", assignees = [ana]) => ({
  ...signedInAsAna,
  "GET /api/v1/tasks/5": () => json(200, task({ assignees })),
  "GET /api/v1/projects/1": () => json(200, project({ myRole: role })),
  "GET /api/v1/projects/1/members": () => json(200, MEMBERS),
});

const panel = () => screen.findByRole("region", { name: "Assignees" });

describe("assignees on the task page", () => {
  afterEach(() => vi.restoreAllMocks());

  it("offers only project members who are not Viewers and not yet assigned", async () => {
    mockApi(routes("OWNER"));
    renderApp("/tasks/5");

    const select = await within(await panel()).findByLabelText("Assign to");
    const options = within(select).getAllByRole("option").map((option) => option.textContent);

    expect(options).toEqual(["Select a person", "Rui Costa"]); // not Ana (assigned), not Sofia (Viewer)
  });

  it("assigns a person and shows the assignees the server returned", async () => {
    const api = mockApi({
      ...routes("MEMBER"),
      "POST /api/v1/tasks/5/assignees": () => json(201, task({ assignees: [ana, rui] })),
    });
    renderApp("/tasks/5");
    const user = userEvent.setup();

    const assignees = within(await panel());
    await user.selectOptions(await assignees.findByLabelText("Assign to"), "2");
    await user.click(assignees.getByRole("button", { name: "Assign" }));

    const list = await within(await panel()).findByRole("list", { name: "Assignees" });
    expect(within(list).getByText("Rui Costa")).toBeInTheDocument();
    expect(within(list).getByText("Ana Silva")).toBeInTheDocument();
    const posts = requests(api, "POST", "/assignees");
    expect(posts).toHaveLength(1);
    expect(JSON.parse(String(posts[0][1]?.body))).toEqual({ userId: 2 });
    // Rui is no longer offered, and everyone eligible is now assigned.
    expect(within(await panel()).getByText("Everyone who can be assigned is already assigned.")).toBeInTheDocument();
  });

  it("keeps the Assign button disabled until a person is chosen", async () => {
    const api = mockApi(routes("OWNER"));
    renderApp("/tasks/5");

    const assignees = within(await panel());
    await assignees.findByLabelText("Assign to");
    expect(assignees.getByRole("button", { name: "Assign" })).toBeDisabled();
    expect(requests(api, "POST", "/assignees")).toHaveLength(0);
  });

  it("shows the server's reason and leaves the assignees unchanged when assigning fails", async () => {
    mockApi({
      ...routes("OWNER"),
      "POST /api/v1/tasks/5/assignees": () =>
        problem(409, "This user is already assigned to the task.", { code: "ALREADY_ASSIGNED" }),
    });
    renderApp("/tasks/5");
    const user = userEvent.setup();

    const assignees = within(await panel());
    await user.selectOptions(await assignees.findByLabelText("Assign to"), "2");
    await user.click(assignees.getByRole("button", { name: "Assign" }));

    expect(await assignees.findByText("This user is already assigned to the task.")).toBeInTheDocument();
    const list = assignees.getByRole("list", { name: "Assignees" });
    expect(within(list).queryByText("Rui Costa")).not.toBeInTheDocument();
    expect(within(list).getByText("Ana Silva")).toBeInTheDocument();
  });

  it("does not send twice when the button is clicked twice while the request is pending", async () => {
    let release: () => void = () => undefined;
    const gate = new Promise<void>((resolve) => (release = resolve));
    const api = mockApi({
      ...routes("OWNER"),
      "POST /api/v1/tasks/5/assignees": async () => {
        await gate;
        return json(201, task({ assignees: [ana, rui] }));
      },
    });
    renderApp("/tasks/5");
    const user = userEvent.setup();

    const assignees = within(await panel());
    await user.selectOptions(await assignees.findByLabelText("Assign to"), "2");
    const button = assignees.getByRole("button", { name: "Assign" });
    await user.click(button);
    await user.click(button);
    release();

    await vi.waitFor(() =>
      expect(within(screen.getByRole("list", { name: "Assignees" })).getByText("Rui Costa")).toBeInTheDocument(),
    );
    expect(requests(api, "POST", "/assignees")).toHaveLength(1);
  });

  it("unassigns one person and leaves the others", async () => {
    const api = mockApi({
      ...routes("OWNER", [ana, rui]),
      "DELETE /api/v1/tasks/5/assignees/2": () => new Response(null, { status: 204 }),
    });
    renderApp("/tasks/5");
    const user = userEvent.setup();

    await user.click(await within(await panel()).findByRole("button", { name: "Unassign Rui Costa" }));

    await vi.waitFor(() =>
      expect(within(screen.getByRole("list", { name: "Assignees" })).queryByText("Rui Costa")).not.toBeInTheDocument(),
    );
    expect(within(screen.getByRole("list", { name: "Assignees" })).getByText("Ana Silva")).toBeInTheDocument();
    expect(requests(api, "DELETE", "/assignees/2")).toHaveLength(1);
    expect(within(await panel()).getByLabelText("Assign to")).toBeInTheDocument(); // Rui can be offered again
  });

  it("says Unassigned after the last person is removed", async () => {
    mockApi({
      ...routes("OWNER", [ana]),
      "DELETE /api/v1/tasks/5/assignees/1": () => new Response(null, { status: 204 }),
    });
    renderApp("/tasks/5");
    const user = userEvent.setup();

    await user.click(await within(await panel()).findByRole("button", { name: "Unassign Ana Silva" }));

    expect(await within(await panel()).findByText("Unassigned")).toBeInTheDocument();
  });

  it("keeps the person assigned and shows the error when unassigning fails", async () => {
    mockApi({
      ...routes("OWNER", [ana, rui]),
      "DELETE /api/v1/tasks/5/assignees/2": () => problem(500, "Internal error."),
    });
    renderApp("/tasks/5");
    const user = userEvent.setup();

    await user.click(await within(await panel()).findByRole("button", { name: "Unassign Rui Costa" }));

    expect(await within(await panel()).findByRole("alert")).toBeInTheDocument();
    expect(within(screen.getByRole("list", { name: "Assignees" })).getByText("Rui Costa")).toBeInTheDocument();
  });

  it("shows a Viewer the assignees but no controls, and does not ask for the member list", async () => {
    const api = mockApi(routes("VIEWER", [ana, rui]));
    renderApp("/tasks/5");

    const assignees = within(await panel());
    expect(await assignees.findByText("Rui Costa")).toBeInTheDocument();
    expect(assignees.queryByLabelText("Assign to")).not.toBeInTheDocument();
    expect(assignees.queryByRole("button")).not.toBeInTheDocument();
    expect(requests(api, "GET", "/members")).toHaveLength(0);
  });

  it("shows an error with a retry when the people cannot be loaded, and the rest of the page still works", async () => {
    let attempts = 0;
    mockApi({
      ...routes("OWNER"),
      "GET /api/v1/projects/1/members": () => (++attempts === 1 ? problem(500, "Internal error.") : json(200, MEMBERS)),
    });
    renderApp("/tasks/5");
    const user = userEvent.setup();

    expect(await screen.findByRole("heading", { level: 1, name: "Write the project brief" })).toBeInTheDocument();
    const assignees = within(await panel());
    expect(await assignees.findByText("Could not load the people to assign.")).toBeInTheDocument();
    expect(assignees.getByText("Ana Silva")).toBeInTheDocument(); // the assignees are still shown
    await user.click(assignees.getByRole("button", { name: "Try again" }));

    expect(await assignees.findByLabelText("Assign to")).toBeInTheDocument();
  });
});

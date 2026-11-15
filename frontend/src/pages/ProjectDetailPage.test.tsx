import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { json, mockApi, problem, project, renderApp, requests, signedInAsAna } from "../test-utils";

describe("project page", () => {
  afterEach(() => vi.restoreAllMocks());

  it("shows the project with its role, description, counts and progress", async () => {
    mockApi({ ...signedInAsAna, "GET /api/v1/projects/1": () => json(200, project()) });

    renderApp("/projects/1");

    expect(await screen.findByRole("heading", { level: 1, name: "Website redesign" })).toBeInTheDocument();
    expect(screen.getByText("Owner")).toBeInTheDocument();
    expect(screen.getByText("New landing page and style guide.")).toBeInTheDocument();
    expect(screen.getByRole("list", { name: "Tasks per status" })).toHaveTextContent(/Done\s*6/);
    expect(screen.getByText("Progress: 50%")).toBeInTheDocument();
  });

  it("says so when the project has no description", async () => {
    mockApi({ ...signedInAsAna, "GET /api/v1/projects/1": () => json(200, project({ description: null })) });

    renderApp("/projects/1");

    expect(await screen.findByText("No description.")).toBeInTheDocument();
  });

  it("offers Edit to the Owner", async () => {
    mockApi({ ...signedInAsAna, "GET /api/v1/projects/1": () => json(200, project({ myRole: "OWNER" })) });

    renderApp("/projects/1");
    expect(await screen.findByRole("button", { name: "Edit" })).toBeInTheDocument();
  });

  it.each([
    ["2", "Member"],
    ["3", "Viewer"],
  ])("does not offer Edit to a %s (%s)", async (id, role) => {
    mockApi({
      ...signedInAsAna,
      [`GET /api/v1/projects/${id}`]: () =>
        json(200, project({ id: Number(id), myRole: role.toUpperCase() as "MEMBER" | "VIEWER" })),
    });

    renderApp(`/projects/${id}`);

    expect(await screen.findByText(role)).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Edit" })).not.toBeInTheDocument();
  });

  it("edits the project and shows what the server confirmed", async () => {
    const api = mockApi({
      ...signedInAsAna,
      "GET /api/v1/projects/1": () => json(200, project()),
      "PATCH /api/v1/projects/1": ({ body }) =>
        json(200, project({ name: (body as { name: string }).name, description: "Server version" })),
    });
    renderApp("/projects/1");
    const user = userEvent.setup();

    await user.click(await screen.findByRole("button", { name: "Edit" }));
    const dialog = screen.getByRole("dialog", { name: "Edit project" });
    expect(within(dialog).getByLabelText("Name")).toHaveValue("Website redesign");
    expect(within(dialog).getByLabelText("Description (optional)")).toHaveValue("New landing page and style guide.");

    await user.clear(within(dialog).getByLabelText("Name"));
    await user.type(within(dialog).getByLabelText("Name"), "Website relaunch");
    await user.click(within(dialog).getByRole("button", { name: "Save changes" }));

    expect(await screen.findByRole("heading", { level: 1, name: "Website relaunch" })).toBeInTheDocument();
    expect(screen.getByText("Server version")).toBeInTheDocument();
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(requests(api, "PATCH", "/projects/1")[0][1]?.body).toBe(
      JSON.stringify({ name: "Website relaunch", description: "New landing page and style guide." }),
    );
  });

  it("explains that the project is gone, without saying whether it ever existed", async () => {
    mockApi({
      ...signedInAsAna,
      "GET /api/v1/projects/99": () => problem(404, "Resource not found."),
    });

    renderApp("/projects/99");

    expect(
      await screen.findByText("This project no longer exists, or you no longer have access to it."),
    ).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Back to my projects" })).toHaveAttribute("href", "/");
  });
});

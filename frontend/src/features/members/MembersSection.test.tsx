import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import type { Member } from "../../api/members";
import { json, mockApi, problem, project, renderApp, requests, signedInAsAna, taskPageOf } from "../../test-utils";

const ANA: Member = { userId: 1, name: "Ana Silva", email: "ana@example.org", role: "OWNER", addedAt: "2026-11-01T10:00:00Z" };
const RUI: Member = { userId: 2, name: "Rui Costa", email: "rui@example.org", role: "MEMBER", addedAt: "2026-11-02T10:00:00Z" };
const SOFIA: Member = { userId: 3, name: "Sofia Lima", email: "sofia@example.org", role: "VIEWER", addedAt: "2026-11-03T10:00:00Z" };

const MEMBERS_URL = "GET /api/v1/projects/1/members";

/** The routes every test needs: the user, the project in the given role, an empty task list. */
const pageAs = (role: "OWNER" | "MEMBER" | "VIEWER", members: Member[] = [ANA, RUI, SOFIA]) => ({
  ...signedInAsAna,
  "GET /api/v1/projects/1": () => json(200, project({ myRole: role })),
  "GET /api/v1/projects/1/tasks": () => json(200, taskPageOf([])),
  [MEMBERS_URL]: () => json(200, members),
});

const panel = () => screen.findByRole("region", { name: "Members" });

describe("members on the project page", () => {
  afterEach(() => vi.restoreAllMocks());

  it("shows every member with their email and role, and no controls to a Member or a Viewer", async () => {
    for (const role of ["MEMBER", "VIEWER"] as const) {
      mockApi(pageAs(role));
      const { unmount } = renderApp("/projects/1");

      const members = within(await panel());
      expect(await members.findByText("Rui Costa")).toBeInTheDocument();
      expect(members.getByText("sofia@example.org")).toBeInTheDocument();
      expect(members.getByText("Owner")).toBeInTheDocument();
      expect(members.queryByRole("button", { name: /add member/i })).not.toBeInTheDocument();
      expect(members.queryByRole("button", { name: /^Remove/ })).not.toBeInTheDocument();
      expect(members.queryByRole("combobox")).not.toBeInTheDocument();

      unmount();
      vi.restoreAllMocks();
    }
  });

  it("gives the Owner a role select and Remove for everyone except the Owner", async () => {
    mockApi(pageAs("OWNER"));
    renderApp("/projects/1");

    const members = within(await panel());
    expect(await members.findByRole("button", { name: "Add member" })).toBeInTheDocument();
    expect(members.getByRole("combobox", { name: "Role of Rui Costa" })).toHaveValue("MEMBER");
    expect(members.getByRole("combobox", { name: "Role of Sofia Lima" })).toHaveValue("VIEWER");
    expect(members.getByRole("button", { name: "Remove Rui Costa" })).toBeInTheDocument();
    expect(members.queryByRole("combobox", { name: "Role of Ana Silva" })).not.toBeInTheDocument();
    expect(members.queryByRole("button", { name: "Remove Ana Silva" })).not.toBeInTheDocument();
  });

  it("shows a plain error with a retry when the list cannot be loaded", async () => {
    let attempts = 0;
    mockApi({
      ...pageAs("OWNER"),
      [MEMBERS_URL]: () => (++attempts === 1 ? problem(500, "Internal error.") : json(200, [ANA, RUI])),
    });
    renderApp("/projects/1");
    const user = userEvent.setup();

    const members = within(await panel());
    expect(await members.findByText("Could not load the members.")).toBeInTheDocument();
    await user.click(members.getByRole("button", { name: "Try again" }));

    expect(await members.findByText("Rui Costa")).toBeInTheDocument();
  });

  describe("adding", () => {
    it("adds a member by email and shows the list as the server returns it", async () => {
      let added = false;
      const api = mockApi({
        ...pageAs("OWNER"),
        [MEMBERS_URL]: () => json(200, added ? [ANA, RUI] : [ANA]),
        "POST /api/v1/projects/1/members": () => {
          added = true;
          return json(201, RUI);
        },
      });
      renderApp("/projects/1");
      const user = userEvent.setup();

      await user.click(await within(await panel()).findByRole("button", { name: "Add member" }));
      const dialog = screen.getByRole("dialog", { name: "Add member" });
      await user.type(within(dialog).getByLabelText("Email"), "  rui@example.org ");
      await user.selectOptions(within(dialog).getByLabelText("Role"), "VIEWER");
      await user.click(within(dialog).getByRole("button", { name: "Add member" }));

      expect(await within(await panel()).findByText("Rui Costa")).toBeInTheDocument();
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
      const posts = requests(api, "POST", "/members");
      expect(posts).toHaveLength(1);
      expect(JSON.parse(String(posts[0][1]?.body))).toEqual({ email: "rui@example.org", role: "VIEWER" });
    });

    it("rejects an empty email without contacting the server", async () => {
      const api = mockApi(pageAs("OWNER"));
      renderApp("/projects/1");
      const user = userEvent.setup();

      await user.click(await within(await panel()).findByRole("button", { name: "Add member" }));
      const dialog = screen.getByRole("dialog", { name: "Add member" });
      await user.type(within(dialog).getByLabelText("Email"), "   ");
      await user.click(within(dialog).getByRole("button", { name: "Add member" }));

      expect(within(dialog).getByText("Must not be empty.")).toBeInTheDocument();
      expect(requests(api, "POST", "/members")).toHaveLength(0);
    });

    it.each([
      [422, "No user has that email."],
      [409, "This user is already a member of the project."],
    ])("shows the server's %s reason next to the field and keeps what was typed", async (status, message) => {
      mockApi({ ...pageAs("OWNER"), "POST /api/v1/projects/1/members": () => problem(status, message) });
      renderApp("/projects/1");
      const user = userEvent.setup();

      await user.click(await within(await panel()).findByRole("button", { name: "Add member" }));
      const dialog = screen.getByRole("dialog", { name: "Add member" });
      await user.type(within(dialog).getByLabelText("Email"), "someone@example.org");
      await user.click(within(dialog).getByRole("button", { name: "Add member" }));

      expect(await within(dialog).findByText(message)).toBeInTheDocument();
      expect(within(dialog).getByLabelText("Email")).toHaveValue("someone@example.org");
      expect(screen.getByRole("dialog", { name: "Add member" })).toBeInTheDocument();
    });

    it("does not send twice when the button is clicked twice while the request is pending", async () => {
      let release: () => void = () => undefined;
      const gate = new Promise<void>((resolve) => (release = resolve));
      const api = mockApi({
        ...pageAs("OWNER"),
        "POST /api/v1/projects/1/members": async () => {
          await gate;
          return json(201, RUI);
        },
      });
      renderApp("/projects/1");
      const user = userEvent.setup();

      await user.click(await within(await panel()).findByRole("button", { name: "Add member" }));
      const dialog = screen.getByRole("dialog", { name: "Add member" });
      await user.type(within(dialog).getByLabelText("Email"), "rui@example.org");
      const submit = within(dialog).getByRole("button", { name: "Add member" });
      await user.click(submit);
      await user.click(within(dialog).getByRole("button", { name: "Adding…" }));
      release();

      await vi.waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
      expect(requests(api, "POST", "/members")).toHaveLength(1);
    });
  });

  describe("changing a role", () => {
    it("sends the new role and shows what the server confirmed", async () => {
      const api = mockApi({
        ...pageAs("OWNER"),
        "PATCH /api/v1/projects/1/members/2": () => json(200, { ...RUI, role: "VIEWER" }),
      });
      renderApp("/projects/1");
      const user = userEvent.setup();

      const select = await within(await panel()).findByRole("combobox", { name: "Role of Rui Costa" });
      await user.selectOptions(select, "VIEWER");

      await vi.waitFor(() => expect(select).toHaveValue("VIEWER"));
      const patches = requests(api, "PATCH", "/members/2");
      expect(patches).toHaveLength(1);
      expect(JSON.parse(String(patches[0][1]?.body))).toEqual({ role: "VIEWER" });
    });

    it("keeps the old role and shows the error when the server refuses", async () => {
      mockApi({
        ...pageAs("OWNER"),
        "PATCH /api/v1/projects/1/members/2": () => problem(403, "You do not have permission to do this."),
      });
      renderApp("/projects/1");
      const user = userEvent.setup();

      const select = await within(await panel()).findByRole("combobox", { name: "Role of Rui Costa" });
      await user.selectOptions(select, "VIEWER");

      expect(await within(await panel()).findByText("You do not have permission to do this.")).toBeInTheDocument();
      expect(select).toHaveValue("MEMBER");
    });
  });

  describe("removing", () => {
    it("asks for confirmation, and cancelling removes nobody", async () => {
      const api = mockApi(pageAs("OWNER"));
      renderApp("/projects/1");
      const user = userEvent.setup();

      await user.click(await within(await panel()).findByRole("button", { name: "Remove Rui Costa" }));
      const dialog = screen.getByRole("dialog", { name: "Remove Rui Costa?" });
      await user.click(within(dialog).getByRole("button", { name: "Cancel" }));

      expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
      expect(requests(api, "DELETE", "/members")).toHaveLength(0);
      expect(within(await panel()).getByText("Rui Costa")).toBeInTheDocument();
    });

    it("removes the member and shows the list without them", async () => {
      let removed = false;
      const api = mockApi({
        ...pageAs("OWNER"),
        [MEMBERS_URL]: () => json(200, removed ? [ANA, SOFIA] : [ANA, RUI, SOFIA]),
        "DELETE /api/v1/projects/1/members/2": () => {
          removed = true;
          return new Response(null, { status: 204 });
        },
      });
      renderApp("/projects/1");
      const user = userEvent.setup();

      await user.click(await within(await panel()).findByRole("button", { name: "Remove Rui Costa" }));
      await user.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Remove member" }));

      await vi.waitFor(() => expect(within(screen.getByRole("region", { name: "Members" })).queryByText("Rui Costa")).not.toBeInTheDocument());
      expect(requests(api, "DELETE", "/members/2")).toHaveLength(1);
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    });

    it("keeps the dialog open and shows the error when removal fails, never a success", async () => {
      mockApi({
        ...pageAs("OWNER"),
        "DELETE /api/v1/projects/1/members/2": () => problem(500, "Internal error."),
      });
      renderApp("/projects/1");
      const user = userEvent.setup();

      await user.click(await within(await panel()).findByRole("button", { name: "Remove Rui Costa" }));
      const dialog = screen.getByRole("dialog");
      await user.click(within(dialog).getByRole("button", { name: "Remove member" }));

      expect(await within(dialog).findByRole("alert")).toBeInTheDocument();
      expect(screen.getByRole("dialog")).toBeInTheDocument();
      expect(within(await panel()).getByText("Rui Costa")).toBeInTheDocument();
    });
  });
});

import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { json, mockApi, problem, project, renderApp, requests, signedInAsAna, taskPageOf } from "../../test-utils";

const withProgress = (manual: number | null, role: "OWNER" | "MEMBER" | "VIEWER" = "OWNER") =>
  project({
    myRole: role,
    progress: { computed: 50, manual, effective: manual ?? 50 },
  });

const routes = (current = withProgress(null)) => ({
  ...signedInAsAna,
  "GET /api/v1/projects/1": () => json(200, current),
  "GET /api/v1/projects/1/tasks": () => json(200, taskPageOf([])),
  "GET /api/v1/projects/1/members": () => json(200, []),
});

describe("manual progress on the project page", () => {
  afterEach(() => vi.restoreAllMocks());

  it("offers the Owner 'Set manual progress' when none is set, and nothing to clear", async () => {
    mockApi(routes());
    renderApp("/projects/1");

    expect(await screen.findByRole("button", { name: "Set manual progress" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Use computed value" })).not.toBeInTheDocument();
    expect(screen.getByText("Progress: 50%")).toBeInTheDocument();
  });

  it("shows a set value as manual with the computed value alongside, and offers to change or clear it", async () => {
    mockApi(routes(withProgress(80)));
    renderApp("/projects/1");

    expect(await screen.findByText("Progress: 80% (manual)")).toBeInTheDocument();
    expect(screen.getByText("Computed from tasks: 50%")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Change manual progress" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Use computed value" })).toBeInTheDocument();
  });

  it.each(["MEMBER", "VIEWER"] as const)("shows a %s the progress but no controls", async (role) => {
    mockApi(routes(withProgress(80, role)));
    renderApp("/projects/1");

    expect(await screen.findByText("Progress: 80% (manual)")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /manual progress/i })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Use computed value" })).not.toBeInTheDocument();
  });

  describe("setting", () => {
    it("sends the whole number and shows the project the server returned", async () => {
      const api = mockApi({
        ...routes(),
        "PUT /api/v1/projects/1/progress-override": () => json(200, withProgress(80)),
      });
      renderApp("/projects/1");
      const user = userEvent.setup();

      await user.click(await screen.findByRole("button", { name: "Set manual progress" }));
      const dialog = screen.getByRole("dialog", { name: "Set manual progress" });
      expect(within(dialog).getByLabelText("Progress (%)")).toHaveValue("50"); // starts from the current value
      await user.clear(within(dialog).getByLabelText("Progress (%)"));
      await user.type(within(dialog).getByLabelText("Progress (%)"), " 80 ");
      await user.click(within(dialog).getByRole("button", { name: "Save" }));

      expect(await screen.findByText("Progress: 80% (manual)")).toBeInTheDocument();
      expect(screen.getByText("Computed from tasks: 50%")).toBeInTheDocument();
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
      const puts = requests(api, "PUT", "/progress-override");
      expect(puts).toHaveLength(1);
      expect(JSON.parse(String(puts[0][1]?.body))).toEqual({ percentage: 80 });
    });

    it.each([
      ["", "Must not be empty."],
      ["   ", "Must not be empty."],
      ["80.5", "Must be a whole number from 0 to 100."],
      ["abc", "Must be a whole number from 0 to 100."],
      ["-5", "Must be a whole number from 0 to 100."],
      ["101", "Must be a whole number from 0 to 100."],
      ["1000", "Must be a whole number from 0 to 100."],
      ["8e1", "Must be a whole number from 0 to 100."],
    ])("refuses %j without contacting the server", async (typed, message) => {
      const api = mockApi(routes());
      renderApp("/projects/1");
      const user = userEvent.setup();

      await user.click(await screen.findByRole("button", { name: "Set manual progress" }));
      const dialog = screen.getByRole("dialog", { name: "Set manual progress" });
      await user.clear(within(dialog).getByLabelText("Progress (%)"));
      if (typed) {
        await user.type(within(dialog).getByLabelText("Progress (%)"), typed);
      }
      await user.click(within(dialog).getByRole("button", { name: "Save" }));

      expect(within(dialog).getByText(message)).toBeInTheDocument();
      expect(requests(api, "PUT", "/progress-override")).toHaveLength(0);
    });

    it.each(["0", "100"])("accepts the boundary %s", async (typed) => {
      const api = mockApi({
        ...routes(),
        "PUT /api/v1/projects/1/progress-override": () => json(200, withProgress(Number(typed))),
      });
      renderApp("/projects/1");
      const user = userEvent.setup();

      await user.click(await screen.findByRole("button", { name: "Set manual progress" }));
      const dialog = screen.getByRole("dialog", { name: "Set manual progress" });
      await user.clear(within(dialog).getByLabelText("Progress (%)"));
      await user.type(within(dialog).getByLabelText("Progress (%)"), typed);
      await user.click(within(dialog).getByRole("button", { name: "Save" }));

      expect(await screen.findByText(`Progress: ${typed}% (manual)`)).toBeInTheDocument();
      expect(requests(api, "PUT", "/progress-override")).toHaveLength(1);
    });

    it("keeps the dialog and what was typed, and shows the reason, when the server refuses", async () => {
      mockApi({
        ...routes(),
        "PUT /api/v1/projects/1/progress-override": () => problem(403, "You do not have permission to do this."),
      });
      renderApp("/projects/1");
      const user = userEvent.setup();

      await user.click(await screen.findByRole("button", { name: "Set manual progress" }));
      const dialog = screen.getByRole("dialog", { name: "Set manual progress" });
      await user.clear(within(dialog).getByLabelText("Progress (%)"));
      await user.type(within(dialog).getByLabelText("Progress (%)"), "70");
      await user.click(within(dialog).getByRole("button", { name: "Save" }));

      expect(await within(dialog).findByText("You do not have permission to do this.")).toBeInTheDocument();
      expect(within(dialog).getByLabelText("Progress (%)")).toHaveValue("70");
      expect(screen.getByText("Progress: 50%")).toBeInTheDocument(); // nothing changed on the page
    });

    it("does not send twice when Save is clicked twice while the request is pending", async () => {
      let release: () => void = () => undefined;
      const gate = new Promise<void>((resolve) => (release = resolve));
      const api = mockApi({
        ...routes(),
        "PUT /api/v1/projects/1/progress-override": async () => {
          await gate;
          return json(200, withProgress(60));
        },
      });
      renderApp("/projects/1");
      const user = userEvent.setup();

      await user.click(await screen.findByRole("button", { name: "Set manual progress" }));
      const dialog = screen.getByRole("dialog", { name: "Set manual progress" });
      await user.clear(within(dialog).getByLabelText("Progress (%)"));
      await user.type(within(dialog).getByLabelText("Progress (%)"), "60");
      await user.click(within(dialog).getByRole("button", { name: "Save" }));
      await user.click(within(dialog).getByRole("button", { name: "Saving…" }));
      release();

      expect(await screen.findByText("Progress: 60% (manual)")).toBeInTheDocument();
      expect(requests(api, "PUT", "/progress-override")).toHaveLength(1);
    });

    it("changes nothing when the dialog is cancelled", async () => {
      const api = mockApi(routes(withProgress(80)));
      renderApp("/projects/1");
      const user = userEvent.setup();

      await user.click(await screen.findByRole("button", { name: "Change manual progress" }));
      await user.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Cancel" }));

      expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
      expect(requests(api, "PUT", "/progress-override")).toHaveLength(0);
      expect(screen.getByText("Progress: 80% (manual)")).toBeInTheDocument();
    });
  });

  describe("clearing", () => {
    it("returns to the computed value, shown without the manual label", async () => {
      const api = mockApi({
        ...routes(withProgress(80)),
        "DELETE /api/v1/projects/1/progress-override": () => json(200, withProgress(null)),
      });
      renderApp("/projects/1");
      const user = userEvent.setup();

      await user.click(await screen.findByRole("button", { name: "Use computed value" }));

      expect(await screen.findByText("Progress: 50%")).toBeInTheDocument();
      expect(screen.queryByText(/manual\)/)).not.toBeInTheDocument();
      expect(screen.queryByRole("button", { name: "Use computed value" })).not.toBeInTheDocument();
      expect(screen.getByRole("button", { name: "Set manual progress" })).toBeInTheDocument();
      expect(requests(api, "DELETE", "/progress-override")).toHaveLength(1);
    });

    it("keeps the manual value and shows the error when clearing fails", async () => {
      mockApi({
        ...routes(withProgress(80)),
        "DELETE /api/v1/projects/1/progress-override": () => problem(500, "Internal error."),
      });
      renderApp("/projects/1");
      const user = userEvent.setup();

      await user.click(await screen.findByRole("button", { name: "Use computed value" }));

      expect(await screen.findByRole("alert")).toBeInTheDocument();
      expect(screen.getByText("Progress: 80% (manual)")).toBeInTheDocument();
    });
  });
});

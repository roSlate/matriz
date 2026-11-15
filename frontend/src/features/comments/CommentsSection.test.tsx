import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import type { Comment } from "../../api/comments";
import { json, mockApi, problem, project, renderApp, requests, signedInAsAna, task } from "../../test-utils";

const comment = (id: number, content: string, author = "Rui Costa"): Comment => ({
  id,
  taskId: 5,
  author: { id: 2, name: author },
  content,
  createdAt: "2026-11-14T10:30:00Z",
});

const pageOf = (items: Comment[], overrides = {}) => ({
  items,
  page: 0,
  size: 20,
  totalItems: items.length,
  totalPages: items.length === 0 ? 0 : 1,
  ...overrides,
});

/** The routes every test needs: the user, the task, the project in a role, and the comment list. */
const routes = (role: "OWNER" | "MEMBER" | "VIEWER", comments = pageOf([])) => ({
  ...signedInAsAna,
  "GET /api/v1/tasks/5": () => json(200, task()),
  "GET /api/v1/projects/1": () => json(200, project({ myRole: role })),
  "GET /api/v1/projects/1/members": () => json(200, []),
  "GET /api/v1/tasks/5/comments": () => json(200, comments),
});

const section = () => screen.findByRole("region", { name: "Comments" });
const lastTwentyFive = Array.from({ length: 25 }, (_, index) => comment(index + 1, `Comment ${index + 1}`));

describe("comments on the task page", () => {
  afterEach(() => vi.restoreAllMocks());

  it("lists the comments oldest first with author and date, and no email", async () => {
    mockApi(routes("MEMBER", pageOf([comment(1, "First one"), comment(2, "Second one", "Ana Silva")])));
    renderApp("/tasks/5");

    const list = within(await within(await section()).findByRole("list", { name: "Comments" }));
    const items = list.getAllByRole("listitem");
    expect(items).toHaveLength(2);
    expect(items[0]).toHaveTextContent("Rui Costa");
    expect(items[0]).toHaveTextContent("First one");
    expect(items[0]).toHaveTextContent("14 Nov 2026");
    expect(items[1]).toHaveTextContent("Ana Silva");
    expect(screen.queryByText(/@example\.org/)).not.toBeInTheDocument();
  });

  it("keeps the line breaks of a comment and shows markup as plain text", async () => {
    mockApi(routes("MEMBER", pageOf([comment(1, "Line one\nLine two <b>not bold</b>")])));
    renderApp("/tasks/5");

    const text = await screen.findByText(/Line one/);
    expect(text.textContent).toBe("Line one\nLine two <b>not bold</b>"); // raw text: line break kept, tags not parsed
    expect(text.querySelector("b")).toBeNull();
  });

  it("says there are no comments yet, which is not an error", async () => {
    mockApi(routes("OWNER"));
    renderApp("/tasks/5");

    expect(await within(await section()).findByText("No comments yet.")).toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it.each(["OWNER", "MEMBER"] as const)("gives an %s the comment form", async (role) => {
    mockApi(routes(role));
    renderApp("/tasks/5");

    expect(await within(await section()).findByLabelText("Add a comment")).toBeInTheDocument();
  });

  it("lets a Viewer read the comments but offers no form", async () => {
    mockApi(routes("VIEWER", pageOf([comment(1, "Readable")])));
    renderApp("/tasks/5");

    const comments = within(await section());
    expect(await comments.findByText("Readable")).toBeInTheDocument();
    expect(comments.queryByLabelText("Add a comment")).not.toBeInTheDocument();
    expect(comments.queryByRole("button", { name: "Comment" })).not.toBeInTheDocument();
    expect(comments.getByText("Viewers can read comments but not write them.")).toBeInTheDocument();
  });

  describe("writing", () => {
    it("posts the trimmed text, shows the server's comment at the end and clears the field", async () => {
      const api = mockApi({
        ...routes("MEMBER", pageOf([comment(1, "Existing")])),
        "POST /api/v1/tasks/5/comments": () => json(201, comment(2, "Server version", "Ana Silva")),
      });
      renderApp("/tasks/5");
      const user = userEvent.setup();

      const comments = within(await section());
      const box = await comments.findByLabelText("Add a comment");
      await user.type(box, "  Waiting for feedback  ");
      await user.click(comments.getByRole("button", { name: "Comment" }));

      const items = await within(await section()).findAllByRole("listitem");
      expect(items).toHaveLength(2);
      expect(items[1]).toHaveTextContent("Server version"); // what the server confirmed
      expect(box).toHaveValue("");
      const posts = requests(api, "POST", "/comments");
      expect(posts).toHaveLength(1);
      expect(JSON.parse(String(posts[0][1]?.body))).toEqual({ content: "Waiting for feedback" });
    });

    it.each([[""], ["   \n  "]])("refuses an empty or whitespace-only text (%j) without a request", async (typed) => {
      const api = mockApi(routes("OWNER"));
      renderApp("/tasks/5");
      const user = userEvent.setup();

      const comments = within(await section());
      const box = await comments.findByLabelText("Add a comment");
      if (typed) {
        await user.type(box, typed);
      }
      await user.click(comments.getByRole("button", { name: "Comment" }));

      expect(comments.getByText("Must not be empty.")).toBeInTheDocument();
      expect(requests(api, "POST", "/comments")).toHaveLength(0);
    });

    it("refuses a text over 2000 characters without a request, and accepts exactly 2000", async () => {
      const api = mockApi({
        ...routes("OWNER"),
        "POST /api/v1/tasks/5/comments": () => json(201, comment(1, "x".repeat(2000))),
      });
      renderApp("/tasks/5");
      const user = userEvent.setup();

      const comments = within(await section());
      const box = await comments.findByLabelText("Add a comment");
      await user.click(box);
      await user.paste("x".repeat(2001));
      await user.click(comments.getByRole("button", { name: "Comment" }));
      expect(comments.getByText("Must be at most 2000 characters.")).toBeInTheDocument();
      expect(requests(api, "POST", "/comments")).toHaveLength(0);

      await user.clear(box);
      await user.click(box);
      await user.paste("x".repeat(2000));
      await user.click(comments.getByRole("button", { name: "Comment" }));
      await vi.waitFor(() => expect(requests(api, "POST", "/comments")).toHaveLength(1));
    });

    it("keeps what was typed and shows the reason when the server refuses", async () => {
      mockApi({
        ...routes("MEMBER"),
        "POST /api/v1/tasks/5/comments": () => problem(403, "You do not have permission to do this."),
      });
      renderApp("/tasks/5");
      const user = userEvent.setup();

      const comments = within(await section());
      const box = await comments.findByLabelText("Add a comment");
      await user.type(box, "My careful text");
      await user.click(comments.getByRole("button", { name: "Comment" }));

      expect(await comments.findByText("You do not have permission to do this.")).toBeInTheDocument();
      expect(box).toHaveValue("My careful text");
      expect(comments.queryByRole("list", { name: "Comments" })).not.toBeInTheDocument(); // nothing was added
    });

    it("shows the server's field error next to the field", async () => {
      mockApi({
        ...routes("MEMBER"),
        "POST /api/v1/tasks/5/comments": () =>
          problem(400, "Validation failed.", { errors: [{ field: "content", message: "Must not be empty." }] }),
      });
      renderApp("/tasks/5");
      const user = userEvent.setup();

      const comments = within(await section());
      await user.type(await comments.findByLabelText("Add a comment"), "Text");
      await user.click(comments.getByRole("button", { name: "Comment" }));

      expect(await comments.findByText("Must not be empty.")).toBeInTheDocument();
    });

    it("does not send twice when the button is clicked twice while the request is pending", async () => {
      let release: () => void = () => undefined;
      const gate = new Promise<void>((resolve) => (release = resolve));
      const api = mockApi({
        ...routes("OWNER"),
        "POST /api/v1/tasks/5/comments": async () => {
          await gate;
          return json(201, comment(1, "Once"));
        },
      });
      renderApp("/tasks/5");
      const user = userEvent.setup();

      const comments = within(await section());
      await user.type(await comments.findByLabelText("Add a comment"), "Once");
      await user.click(comments.getByRole("button", { name: "Comment" }));
      await user.click(comments.getByRole("button", { name: "Posting…" }));
      release();

      expect(await comments.findAllByRole("listitem")).toHaveLength(1);
      expect(requests(api, "POST", "/comments")).toHaveLength(1);
    });
  });

  describe("long conversations", () => {
    const firstPage = pageOf(lastTwentyFive.slice(0, 20), { totalItems: 25, totalPages: 2 });
    const secondPage = pageOf(lastTwentyFive.slice(20), { page: 1, totalItems: 25, totalPages: 2 });
    const pagedRoutes = (extra = {}) => ({
      ...routes("MEMBER"),
      "GET /api/v1/tasks/5/comments": ({ path }: { path: string }) =>
        json(200, path.includes("page=1") ? secondPage : firstPage),
      ...extra,
    });

    it("shows 20 comments and loads the rest with Show more", async () => {
      mockApi(pagedRoutes());
      renderApp("/tasks/5");
      const user = userEvent.setup();

      const comments = within(await section());
      expect(await comments.findAllByRole("listitem")).toHaveLength(20);
      expect(comments.getByText("Showing 20 of 25 comments")).toBeInTheDocument();

      await user.click(comments.getByRole("button", { name: "Show more" }));

      await vi.waitFor(() => expect(comments.getAllByRole("listitem")).toHaveLength(25));
      expect(comments.getAllByRole("listitem")[24]).toHaveTextContent("Comment 25");
      expect(comments.queryByRole("button", { name: "Show more" })).not.toBeInTheDocument();
    });

    it("shows the person their own comment even when older pages were not loaded yet", async () => {
      const newest = comment(99, "My new comment", "Ana Silva");
      const api = mockApi(
        pagedRoutes({
          "GET /api/v1/tasks/5/comments": ({ path }: { path: string }) =>
            json(
              200,
              path.includes("page=1")
                ? pageOf([...lastTwentyFive.slice(20), newest], { page: 1, totalItems: 26, totalPages: 2 })
                : firstPage,
            ),
          "POST /api/v1/tasks/5/comments": () => json(201, newest),
        }),
      );
      renderApp("/tasks/5");
      const user = userEvent.setup();

      const comments = within(await section());
      await user.type(await comments.findByLabelText("Add a comment"), "My new comment");
      await user.click(comments.getByRole("button", { name: "Comment" }));

      await vi.waitFor(() => expect(comments.getAllByRole("listitem")).toHaveLength(26));
      const items = comments.getAllByRole("listitem");
      expect(items[25]).toHaveTextContent("My new comment");
      expect(items.filter((item) => item.textContent?.includes("My new comment"))).toHaveLength(1); // no duplicate
      expect(requests(api, "POST", "/comments")).toHaveLength(1);
    });

    it("keeps the comments and says so when Show more fails, and lets the person try again", async () => {
      let attempts = 0;
      mockApi(
        pagedRoutes({
          "GET /api/v1/tasks/5/comments": ({ path }: { path: string }) =>
            path.includes("page=1")
              ? ++attempts === 1
                ? problem(500, "Internal error.")
                : json(200, secondPage)
              : json(200, firstPage),
        }),
      );
      renderApp("/tasks/5");
      const user = userEvent.setup();

      const comments = within(await section());
      await user.click(await comments.findByRole("button", { name: "Show more" }));
      expect(await comments.findByText("Could not load more comments. Please try again.")).toBeInTheDocument();
      expect(comments.getAllByRole("listitem")).toHaveLength(20);

      await user.click(comments.getByRole("button", { name: "Show more" }));
      await vi.waitFor(() => expect(comments.getAllByRole("listitem")).toHaveLength(25));
    });
  });

  it("shows an error with a retry when the comments cannot be loaded, and the rest of the page still works", async () => {
    let attempts = 0;
    mockApi({
      ...routes("MEMBER"),
      "GET /api/v1/tasks/5/comments": () => (++attempts === 1 ? problem(500, "Internal error.") : json(200, pageOf([comment(1, "Back")]))),
    });
    renderApp("/tasks/5");
    const user = userEvent.setup();

    expect(await screen.findByRole("heading", { level: 1, name: "Write the project brief" })).toBeInTheDocument();
    const comments = within(await section());
    expect(await comments.findByText("Could not load the comments.")).toBeInTheDocument();
    expect(comments.queryByLabelText("Add a comment")).not.toBeInTheDocument(); // no form until the list is known
    await user.click(comments.getByRole("button", { name: "Try again" }));

    expect(await comments.findByText("Back")).toBeInTheDocument();
  });
});

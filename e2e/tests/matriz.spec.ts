import { expect, test, type Page } from "@playwright/test";

// These tests use the fictional demo accounts of the "dev" profile (see the README). Selectors are
// roles and labels, as a person would find things, and every wait is a condition, never a delay.
const PASSWORD = "matriz-demo";

async function logIn(page: Page, email: string, password = PASSWORD) {
  await page.goto("/");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password").fill(password);
  await page.getByRole("button", { name: "Log in" }).click();
}

test("a member logs in, opens a project, creates a task and sees it in the list", async ({ page }) => {
  // Unique data, so that the test can be repeated and never depends on an earlier run.
  const title = `E2E task ${Date.now()}`;

  await logIn(page, "ana@example.org");
  await expect(page.getByRole("heading", { name: "My projects" })).toBeVisible();

  await page.getByRole("link", { name: /Website redesign/ }).click();
  await expect(page.getByRole("heading", { level: 1, name: "Website redesign" })).toBeVisible();

  await page.getByRole("button", { name: "New task" }).click();
  const dialog = page.getByRole("dialog", { name: "New task" });
  await dialog.getByLabel("Title").fill(title);
  await dialog.getByRole("button", { name: "Create task" }).click();

  // The new task is in the list without reloading the page, exactly once...
  await expect(page.getByRole("link", { name: title })).toHaveCount(1);
  // ...and it is still there after a reload (it was saved, not only shown).
  await page.reload();
  await expect(page.getByRole("link", { name: title })).toHaveCount(1);

  // Clean up through the interface, which also checks that deleting works.
  await page.getByRole("link", { name: title }).click();
  await page.getByRole("button", { name: "Delete task" }).click();
  await page.getByRole("dialog").getByRole("button", { name: "Delete task" }).click();
  await expect(page.getByRole("heading", { level: 1, name: "Website redesign" })).toBeVisible();
  await expect(page.getByRole("link", { name: title })).toHaveCount(0);
});

test("a wrong password is refused with the generic message and keeps the email", async ({ page }) => {
  await logIn(page, "ana@example.org", "not-the-password");

  await expect(page.getByRole("alert")).toContainText("Invalid email or password.");
  await expect(page.getByLabel("Email")).toHaveValue("ana@example.org");
  await expect(page.getByRole("heading", { name: "Log in" })).toBeVisible();
});

test("a user who is not a member of a project cannot open it", async ({ page }) => {
  // "Team handbook" (project 3 in the demo data) belongs to Sofia; Ana has no access to it.
  await logIn(page, "ana@example.org");
  await expect(page.getByRole("heading", { name: "My projects" })).toBeVisible();
  await expect(page.getByText("Team handbook")).toHaveCount(0); // not even listed

  await page.goto("/projects/3");

  await expect(page.getByText("This project no longer exists, or you no longer have access to it.")).toBeVisible();
  await expect(page.getByText("Team handbook")).toHaveCount(0); // nothing about it is shown
});

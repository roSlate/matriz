import { defineConfig, devices } from "@playwright/test";

// The tests drive an application that is already running (see e2e/README.md). The address can be
// changed with E2E_BASE_URL; by default it is the Docker Compose stack.
export default defineConfig({
  testDir: "./tests",
  // The tests share one database, so they run one after the other.
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  reporter: [["list"], ["html", { open: "never" }]],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? "http://localhost:8088",
    // Evidence only when something fails.
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
});

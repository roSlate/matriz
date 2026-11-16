# Matriz: end-to-end tests

Playwright tests that drive the running application in a real browser (Chromium), the way a person would. They cover: logging in, opening a project, creating a task and seeing it in the list (also after a reload), a refused login, and a user who has no access to a project.

## Requirements

- The whole application running with the demo data, for example `docker compose up -d --build` from the repository root (see the main [README](../README.md)); the demo data needs `SPRING_PROFILES_ACTIVE=dev` in `.env`.
- Node 20.19 or later.

## Run

```bash
cd e2e
npm install
npx playwright install chromium     # once: downloads the browser (about 170 MB)
npm test
```

The tests open `http://localhost:8088` by default. To test another address, for example the Vite development server, set it: `E2E_BASE_URL=http://localhost:5173 npm test` (the backend must then run on port 8080).

When a test fails, the report with a screenshot and a trace is in `playwright-report/`; open it with `npx playwright show-report`.

## How the tests are written

- **Stable selectors:** roles and labels (`getByRole`, `getByLabel`), never CSS classes or positions.
- **No fixed waits:** every wait is a condition that Playwright retries until it holds.
- **Isolated data:** the task is created with a unique title and deleted at the end through the interface, so the tests can be repeated and do not depend on earlier runs. The tests only read the other demo data.
- **They use the demo accounts** (`ana@example.org` and the password in the main README); they must never run against real data.
- **They guard something:** the test of the missing access was checked with a deliberate negative control, running it as a user who does have access, which made it fail.

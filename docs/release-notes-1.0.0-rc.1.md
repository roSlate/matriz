# Matriz 1.0.0-rc.1: release notes

Release candidate, 2026-11-16. Git tag: `v1.0.0-rc.1`. The changes in detail are in [../CHANGELOG.md](../CHANGELOG.md).

This is a **release candidate**, not a production release: the MVP is complete and verified, and nothing has been published. The version follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html): `1.0.0` is the first release of the complete MVP, and `rc.1` marks the first candidate for it. What would still have to be done before a `1.0.0` that faces real users is listed in section 5.

## 1. What it is

Matriz is a web application where teams organise projects, manage tasks and follow the progress of their work in one place (see the [README](../README.md)). This candidate contains everything the approved specification asks for:

| Area | What a user can do |
|---|---|
| Access | Log in and out; the session ends after 30 minutes of inactivity, with a clear message. |
| Projects | See their projects with role, task counts and progress; search by name; create, edit and delete (Owner). |
| Members | Add people by email with the role Member or Viewer, change roles, remove them (Owner). |
| Tasks | Create, edit, delete; set status and priority; filter by both; page through the list. |
| Assignees | Assign several Owners and Members to a task, and remove each assignment. |
| Comments | Comment on a task; each comment shows its author's name and date. |
| Progress | See the progress computed from the tasks, or a manual value set by the Owner, always labelled. |

Every rule is enforced on the server, also for direct API calls: a user without access to a project cannot even learn that it exists (`404`), and a user with too low a role is refused (`403`).

## 2. How to run it

You need Docker Desktop. From the repository root:

```bash
cp .env.example .env          # then set the same password in POSTGRES_PASSWORD and DB_PASSWORD
docker compose up -d --build
```

Open http://localhost:8088 and log in with a demo account (password `matriz-demo`): `ana@example.org` (Owner of "Website redesign"), `rui@example.org` (Owner of "Client onboarding") or `sofia@example.org` (Owner of "Team handbook", Viewer elsewhere). The first build takes several minutes because the base images and dependencies are downloaded. Stop with `docker compose down` (the data stays) or `docker compose down -v` (the data is deleted). The full instructions, including running without Docker, are in the [README](../README.md).

The demo data and accounts are loaded because `.env.example` sets `SPRING_PROFILES_ACTIVE=dev`. They are fictional and for demonstration only.

## 3. What changed since the delivery of the implementation (Ficha 03)

The tag `ficha-03-delivery` marks the end of the implementation stage. Since then:

- **Docker:** images for the backend and the frontend, and `compose.yaml` with the database, the backend and the web service, healthchecks and a persistent volume.
- **Continuous integration:** a GitHub Actions pipeline that runs the tests and, only when they pass, builds the images, smoke-tests the stack and runs the end-to-end tests.
- **Tests:** unit tests of the validation rules (10), and end-to-end tests with Playwright (4).
- **Security review:** security headers and a Content-Security-Policy on the pages served by nginx, and no version in the `Server` header; findings and decisions are in [quality-security-review.md](quality-security-review.md).
- **Documents:** the test plan, the changelog, these release notes, and the final report.
- **Version:** `1.0.0-rc.1` (the backend jar is now `matriz-backend-1.0.0-rc.1.jar`).

## 4. Quality evidence

| Check | Result |
|---|---|
| Backend tests (`mvn verify`, real PostgreSQL) | 144 passed |
| Frontend tests, type check, build | 122 passed; clean |
| End-to-end tests (Playwright, Chromium) | 4 passed |
| Scripted check of the nine verification cases, with a real backend restart | 99 of 99 |
| Manual check in a browser by the developer | 16 of 16 |
| Pipeline on GitHub | Green; a deliberate failure was shown to fail the pipeline and skip the image build ([test-plan.md](test-plan.md), section 6) |
| Clean copy of the repository following the README | Built, migrated, started and passed the tests ([verification.md](verification.md), section 8) |

The records are in [verification.md](verification.md), [test-plan.md](test-plan.md) and [quality-security-review.md](quality-security-review.md). The final report is [final-report.md](final-report.md).

## 5. Known limitations, and what a real `1.0.0` would need

- **HTTPS** and the `Secure` flag of the session cookie: the application only runs over HTTP on `localhost`.
- **Protection against repeated login attempts** (rate limiting or lockout): none.
- **A limit on the size of a JSON request body**: none.
- **Keyboard focus** is not trapped inside the dialogs.
- **Supply chain:** the base images use floating tags, and no dependency or image scanner is part of the pipeline.
- **Browsers and devices:** one browser (Chromium) was tested end to end; there are no accessibility, load or concurrency tests.
- **Accounts:** there is no registration or password recovery; the demo accounts are seeded. There is no administrator, and a project has exactly one Owner with no ownership transfer.
- **Deleting a project is permanent.**

## 6. Upgrading

There is no earlier release. The database schema is created by the Flyway migration `V1__create_schema.sql`; the demo data (`V1000`) is applied only with the `dev` profile and must not be used in a real deployment.

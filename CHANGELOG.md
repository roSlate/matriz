# Changelog

All notable changes to Matriz are recorded here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the version numbers follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Matriz is a course project. Nothing was released before the first release candidate; the work that led to it is described by stage in [docs/implementation-plan.md](docs/implementation-plan.md), and the Git tag `ficha-03-delivery` marks the state at the end of the implementation stage (Ficha 03).

## [Unreleased]

Nothing yet.

## [1.0.0-rc.1] - 2026-11-16

The first release candidate: the MVP defined in [docs/requirements.md](docs/requirements.md), complete, verified, and runnable with one command (`docker compose up -d --build`). It is a candidate, not a production release; the known limitations are listed below and in [docs/verification.md](docs/verification.md), section 7. Release notes: [docs/release-notes-1.0.0-rc.1.md](docs/release-notes-1.0.0-rc.1.md).

### Added

- **Access:** login and logout with a server-side session (30 minutes of inactivity), the profile of the signed-in user, CSRF protection, a login page and a "session expired" message.
- **Projects:** a dashboard with the user's role, task counts per status and progress; search by name; create, view, edit and delete (Owner), with paging.
- **Members and roles** (Owner, Member, Viewer): add a member by email, change a role, remove a member; the list is visible to every member of the project.
- **Tasks:** a list with status and priority filters and paging; create, edit, change status and priority, delete; a task page.
- **Assignees:** assign and unassign Owners and Members; assignments are removed when someone is removed or demoted.
- **Comments:** oldest first with "Show more"; written by Owners and Members; shown with the author's name only.
- **Progress:** computed from the tasks, with an optional manual value (0 to 100) set by the Owner, always labelled as manual with the computed value alongside.
- **Authorization on the server for every route**, also for direct API calls: a user without access gets `404`, a user with too low a role gets `403`.
- **Data:** a PostgreSQL schema created by Flyway migrations, and fictional demo data loaded only by the `dev` profile.
- **API contract:** `openapi.yaml` (OpenAPI 3.1) with 23 operations, kept equal to the code.
- **Docker:** a backend image, a frontend image served by nginx that forwards `/api` to the backend, and `compose.yaml` with the database, the backend and the web service, healthchecks and a persistent volume.
- **Continuous integration:** a GitHub Actions pipeline that tests the backend, checks and builds the frontend, and, only when both pass, builds the images, smoke-tests the stack and runs the end-to-end tests. It has read-only permissions, uses no secrets, and pins every action to a commit.
- **Tests:** 144 backend tests (unit and integration, against a real PostgreSQL), 122 frontend tests, 4 end-to-end tests with Playwright, and a scripted check of the nine verification cases (99 checks) with a real backend restart.
- **Documentation:** the specification pack, the architecture and data model, the implementation plan, the verification record, the test plan, the quality and security review, `AGENTS.md`, and this changelog.

### Changed

- The error handler answers unknown routes and bad query values with fixed texts in the standard error format, never with the framework's own wording.
- The JSON mapper refuses unknown fields and also values of the wrong type: a number or a boolean for text, a text for a number, a decimal for a whole number.

### Fixed

These were found by tests, by the review or by the verification, before this candidate:

- A JSON number sent for a text field was saved as text, and a decimal sent for a whole number was cut short (`80.5` became `80`).
- Unknown routes and invalid query values exposed the framework's wording.
- A 401 arriving right after the first render could fail to show the "session expired" message (a timing race in the frontend).
- The pages served by nginx had no security headers and the `Server` header revealed the nginx version.

### Security

- Passwords are stored as BCrypt hashes; sessions are server-side in an HTTP-only cookie with `SameSite=Lax`; requests that change data need the CSRF token.
- The server decides the project owner and the comment author; a request that tries to supply them, or any other unknown field, is refused.
- nginx sets `X-Frame-Options`, `X-Content-Type-Options`, `Referrer-Policy`, `Permissions-Policy` and a strict Content-Security-Policy (same origin only, no inline scripts or styles).
- The containers run as non-root users, only the web service is published on the host (on `127.0.0.1`), and no credential is in any image or in the repository.

### Known limitations

- No protection against repeated login attempts (rate limiting or lockout).
- No HTTPS and therefore no `Secure` flag on the session cookie; both belong to a real deployment.
- No limit on the size of a JSON request body.
- The dialogs do not trap the keyboard focus.
- The base images use floating tags and no dependency audit tool is part of the pipeline.
- The end-to-end tests cover one main flow in one browser (Chromium); there are no accessibility, load or concurrency tests.
- Deleting a project is permanent; a project has exactly one Owner and no ownership transfer.

[Unreleased]: https://github.com/roSlate/matriz/compare/v1.0.0-rc.1...HEAD
[1.0.0-rc.1]: https://github.com/roSlate/matriz/releases/tag/v1.0.0-rc.1

# Matriz: Implementation Plan

Status: draft for Ficha 03, Exercise 1. Related documents: [requirements.md](requirements.md), [user-stories.md](user-stories.md), [constraints.md](constraints.md), [architecture.md](architecture.md), [database.md](database.md), [../openapi.yaml](../openapi.yaml), [ui/README.md](ui/README.md).

This plan turns the approved specification and design into a sequence of increments. Each increment is a vertical slice: it goes through the interface, the API, the business rules and the database, and it is finished only when its acceptance criteria pass and its checks have been run. The status of each increment is updated as the work advances.

Status values: **Planned**, **In progress**, **Done**, **Cut** (dropped, with the reason).

## 1. Scope reconciliation (Ficha 03 versus the approved MVP)

| Topic | Ficha 03 | Our documents | Resolution |
|---|---|---|---|
| Functional scope | Authentication, projects, tasks, assignees, comments, search and filters, dashboard | The same, as US-01 to US-09 and US-11 | Nothing from the ficha is missing. |
| Extras | none | Member management with three roles (US-03); manual progress override (US-10) | Kept. They are the first candidates to cut if time runs short (section 6). |
| Screens | Login, dashboard, projects, project detail, task detail | Login, dashboard that is also the project list, project detail, task detail | The dashboard and project list are one screen, as in the wireframes. |
| Eligible assignees | "Eligible users" left to define | Owners and Members of the task's project (FR-20) | Defined in the specification. |
| Accounts | Seeded demo accounts; registration optional | Seeded accounts only (A-16) | Registration and password recovery stay out. |
| Field limits | Project name 3–100, description 1000; task title 3–150, description 2000; comment 1–2000 | The same (A-13) | Consistent. |
| Tests | At least three automated tests: a validation rule, an authorisation scenario, create then read after persistence | NFR-09 | Consistent; see section 5. |

## 2. Implementation decisions

Decisions already taken are in [architecture.md](architecture.md) (Java and Spring Boot, React with TypeScript, PostgreSQL, Flyway, session cookie, Maven, tests). These are the additional choices needed to start. They were proposed by the analysis and confirmed by the developer.

| Topic | Proposal | Reason |
|---|---|---|
| Repository layout | `backend/` (Maven project) and `frontend/` (Vite project) in the same repository, plus `compose.yaml` and `.env.example` at the root. | One repository, as the ficha requires; simple to run and to containerise later. |
| Java package root | `com.matriz`, with one sub-package per module: `identity`, `project`, `task`, `comment`, plus `common` and `security`. | Mirrors the architecture; short. |
| Database during development | PostgreSQL in a Docker container started by `compose.yaml` (database service only at this stage). Data lives in a named volume. | Reproducible, no local installation. Ficha 05 extends the same file with the application services. |
| Schema | Flyway migrations in `backend/src/main/resources/db/migration`, written from [database.md](database.md). | Reproducible schema (Ficha 03, exercise 2). |
| Demo data | A separate Flyway location (`db/demo`) that is enabled only by the `dev` profile. It creates three fictional users and three projects with different access (see below). | Runs once, never wipes data on later starts, and cannot reach a real deployment (A-16). |
| Configuration | Environment variables read by Spring; `.env.example` lists them with fictional values; the real `.env` is ignored by Git. | S-08. |
| Browser and API | In development the Vite server proxies `/api` to the backend, so the browser sees one origin and the session and CSRF cookies work without CORS rules. | Simplest correct setup for cookie sessions. |
| UI foundation | A small set of reusable components (button, input, select, card, badge, dialog, alert) and CSS variables for colours, spacing and radii, built in the first frontend increment. The visual style from [constraints.md](constraints.md), section 4.1, is applied through those variables. | Reuse is required by the ficha, and the style can then change in one place. |

**Demo data.** Passwords are fictional, development only, and stored as hashes.

| User | Email | Role in "Website redesign" | Role in "Client onboarding" | Role in "Team handbook" |
|---|---|---|---|---|
| Ana Silva | ana@example.org | Owner | Member | none |
| Rui Costa | rui@example.org | Member | Owner | none |
| Sofia Lima | sofia@example.org | Viewer | none | Owner |

This gives every case the stories need: all three roles, a project a user cannot see (Ana and Rui have no access to "Team handbook"), and two users for the cross-user authorisation checks. The seed also adds a few tasks and comments so that the screens have content.

## 3. Target repository layout

```
matriz/
├── README.md, AGENTS.md, openapi.yaml, compose.yaml, .env.example
├── docs/                      (specification, design, this plan, verification)
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/matriz/{identity,project,task,comment,security,common}/
│       ├── main/resources/{application.yml, db/migration, db/demo}
│       └── test/java/com/matriz/...
└── frontend/
    ├── package.json, vite.config.ts
    └── src/{api, components/ui, features/{auth,projects,tasks,comments}, pages}/
```

## 4. Increments

| # | Increment | Stories | Depends on | Priority | Status |
|---|---|---|---|---|---|
| 0 | Skeleton, database, demo data | none (setup) | none | Must | Done |
| 1 | Authentication | US-01 | 0 | Must | Done |
| 2 | Projects: create, list, view, edit | US-02 | 1 | Must | Planned |
| 3 | Members and roles | US-03 | 2 | Must | Planned |
| 4 | Tasks | US-04, US-05, part of US-09 | 3 | Must | Planned |
| 5 | Assignees | US-07 | 4 | Should | Planned |
| 6 | Dashboard, search, project deletion | US-06, rest of US-09, US-11 | 4 | Must | Planned |
| 7 | Comments | US-08 | 4 | Should | Planned |
| 8 | Manual progress override | US-10 | 6 | Should | Planned |
| 9 | Verification, review and delivery | all | all done | Must | Planned |

Milestone, as the ficha names it: **after increment 2**, a user can log in, create a project, see it in the list, and still see it after reloading the page and restarting the backend.

For every increment the working method is the one from the course: ask the AI for an analysis of the relevant code and documents, then a plan; approve it; implement one increment; run the checks; review the diff; only then commit.

### Increment 0: Skeleton, database, demo data (Ficha 03, exercise 2)

- **Backend:** Maven project with Spring Boot (web, security, data JPA, validation, Flyway, PostgreSQL driver, test starter); configuration from environment variables; `dev` profile; migration V1 with the six tables, constraints and indexes of [database.md](database.md); demo-data location; a health endpoint.
- **Frontend:** Vite React TypeScript project; the `/api` proxy; routing with a placeholder page; the base styles (design tokens). The reusable UI components are built in increment 1, with the first real screen.
- **Root:** `compose.yaml` with PostgreSQL; `.env.example`; the commands in the README.
- **Checks:** the database starts; the application starts and connects; migrations apply on an empty database and are not applied twice; restarting does not duplicate or wipe the demo data; the frontend starts and reaches the backend through the proxy.
- **Done when:** the README commands start everything from a clean checkout.
- **Result (done):** Spring Boot 4.1.2 with Java 21 and Maven; React 19, Vite 8 and TypeScript 5.9. Backend `mvn verify`: 8 schema tests passed against a PostgreSQL 17 container (tables; email unique ignoring case; one Owner per project; title limits; status and priority values; assignees must be members; removing a member removes their assignments; deleting a project removes everything inside it). Frontend: type check, 2 unit tests and production build pass. On the development database: the migration applied; the `dev` demo data loaded (3 users, 3 projects, 6 memberships, 18 tasks, 4 assignments, 3 comments); after a restart Flyway reported the schema up to date and the row counts were unchanged; the health endpoint answers, other routes are refused, and the Vite proxy reaches the backend. Not yet checked from a clean copy of the repository (done in increment 9). The temporary security configuration still logs a generated in-memory password; the real configuration replaces it in increment 1.

### Increment 1: Authentication (US-01)

- **Backend:** identity module (user entity and repository, BCrypt, current-user service); security configuration with the session cookie, CSRF cookie and header, 30-minute inactivity timeout; `POST /auth/login`, `POST /auth/logout`, `GET /users/me`; the error format (Problem Details with `code`) and the shared error handler; `401` for everything else.
- **Frontend:** API client (base URL in one place, CSRF header, handling of `401`); login page with validation and the generic error message; the authenticated layout with the header (name and log out); redirect to login with the expired-session message.
- **Acceptance:** AC-01.1 to AC-01.7; CC-02 (no internals in errors).
- **Checks:** valid and invalid credentials (same message); a request without a session is refused; logout invalidates the session; a missing CSRF token is refused; the password hash never appears in any response.
- **Done when:** the demo users can log in and out through the interface.
- **Result (done):** Backend `mvn test`: 22 tests pass (8 schema, 10 API with MockMvc, 4 over real HTTP against the embedded server for cookies and the CSRF handshake). Frontend: type check, production build and 8 tests pass (login validation without contacting the server, whitespace-only password, the server's message kept with the email, successful login, logout, session-expired message, no false "expired" message for a visitor). Checked for real through the Vite proxy with the seeded user: anonymous request `401` with the `XSRF-TOKEN` cookie; login without the token `403`; wrong password and unknown email give identical answers; login works with the email in upper case; `/users/me` with the session `200`; logout `204`; the old session then `401`; an unknown field `400`. Findings during the work: (1) MockMvc does not show the `Set-Cookie` headers, so the first cookie test was wrong, and a second one passed vacuously; both were replaced by real-HTTP tests. (2) Anonymous requests created a session as a side effect of Spring's saved-request feature, now disabled. (3) The CSRF cookie now carries `SameSite=Lax`. Not covered here: the `Secure` flag of the session cookie (needs HTTPS, planned for the delivery stage) and brute-force protection (accepted, constraints.md section 6).

### Increment 2: Projects: create, list, view, edit (US-02)

- **Backend:** project module (project and membership entities, repositories); the **access step** and the **access policy** (architecture.md, section 5): resolve the project, look up the membership, `404` if none; creation of the project and its Owner membership in one transaction; `POST /projects`, `GET /projects` (without search yet), `GET /projects/{projectId}`, `PATCH /projects/{projectId}`; the `TaskCounts` port with a first implementation that returns zeros until tasks exist; the progress computation.
- **Frontend:** project list on the dashboard, create-project form, project detail page (header only), edit-project form; loading, empty and error states.
- **Acceptance:** AC-02.1 to AC-02.5; AC-06.1 (only accessible projects); CC-05 for the project routes.
- **Checks:** the owner is the creator and cannot be supplied by the client; Ana does not see "Team handbook"; a non-member gets `404`, also for an invalid body; a Member or Viewer cannot edit; limits and whitespace-only names are rejected.
- **Done when (the milestone):** log in, create a project, see it in the list, reload the page, restart the backend and see it still there.

### Increment 3: Members and roles (US-03)

- **Backend:** `GET`, `POST`, `PATCH`, `DELETE` on `/projects/{projectId}/members`; the rules: add by email (`422` if unknown, `409` if already a member), one Owner, the Owner cannot be removed or demoted; the membership event (removal and demotion) published by the project module; role checks completed for all project operations.
- **Frontend:** members panel on the project detail page, with role select, remove, and add by email; controls shown according to the user's role.
- **Acceptance:** AC-03.1 to AC-03.9; EC-02, EC-06, EC-08, EC-11 (the task-related parts complete in increment 5).
- **Checks:** all role combinations on member operations; the Owner rules; removing a member removes their access at once.
- **Done when:** the Owner can manage members through the interface and other roles cannot.

### Increment 4: Tasks (US-04, US-05, filters from US-09)

- **Backend:** task module (entity, repository, service); `GET` and `POST` on `/projects/{projectId}/tasks` with the status and priority filters and pagination; `GET`, `PATCH`, `DELETE` on `/tasks/{taskId}`; the access step extended to resolve a task to its project; the real `TaskCounts` implementation.
- **Frontend:** task list with filters and pagination on the project detail page; new-task form with validation messages; task detail page (data, edit, status and priority, delete with confirmation); counts and progress now real.
- **Acceptance:** AC-04.1 to AC-04.7; AC-05.1 to AC-05.5; AC-09.2 to AC-09.4; FR-26; EC-01 (task titles).
- **Checks:** a new task appears without a reload, once, and survives a reload and a restart; invalid and whitespace-only titles are rejected; a Viewer cannot write; a task of another project is not reachable by changing the identifier; filters combine.
- **Done when:** a project's tasks can be fully managed through the interface.

### Increment 5: Assignees (US-07)

- **Backend:** `POST /tasks/{taskId}/assignees` and `DELETE /tasks/{taskId}/assignees/{userId}`; eligibility check (Owner or Member of the project); `409` and `422` cases; the handler in the task module for the membership event (assignment cleanup on removal and demotion).
- **Frontend:** assignees panel on the task detail page; the list of people to assign taken from the members list, without Viewers; "Unassigned" state.
- **Acceptance:** AC-07.1 to AC-07.7; AC-03.5 and AC-03.8 (assignment cleanup); EC-02, EC-04, EC-11.
- **Checks:** no duplicates; Viewers and outsiders are refused by the server; removing and demoting a member remove their assignments but keep the tasks.
- **Done when:** several people can be assigned and unassigned, and the cleanup rules hold.

### Increment 6: Dashboard, search, project deletion (US-06, rest of US-09, US-11)

- **Backend:** search by name on `GET /projects`; pagination of the project list; `DELETE /projects/{projectId}` with the database cascade; counts and progress on every project item.
- **Frontend:** dashboard cards with counts per status and progress, search box, pagination, delete-project dialog; counts update after task changes.
- **Acceptance:** AC-06.1 to AC-06.6; AC-09.1, AC-09.3; AC-11.1 to AC-11.5; EC-09.
- **Checks:** counts equal the real totals (not one page) and sum correctly; the rounding rule; a deleted project leaves no orphaned data (checked in the database); only the Owner can delete.
- **Done when:** the dashboard matches the wireframe and deletion is clean.

### Increment 7: Comments (US-08)

- **Backend:** comment module; `GET` and `POST` on `/tasks/{taskId}/comments` with pagination, author and date from the server, author name only (no email).
- **Frontend:** comment list and form on the task detail page, with the "Show more" control and validation.
- **Acceptance:** AC-08.1 to AC-08.6; CC-01 exception for comment authors.
- **Checks:** whitespace-only and over-long comments are rejected; author and date cannot be supplied by the client; comments survive reopening and the author's removal from the project; Viewers cannot comment.

### Increment 8: Manual progress override (US-10)

- **Backend:** `PUT` and `DELETE /projects/{projectId}/progress-override`; the effective progress logic; Owner only.
- **Frontend:** the "Set manual progress" control, the "manual" label and the computed value alongside, on both the project detail page and the dashboard card.
- **Acceptance:** AC-10.1 to AC-10.5.

### Increment 9: Verification, review and delivery (Ficha 03, exercises 8 and 9)

- Write `docs/verification.md` (created in this increment): for the nine cases of the ficha, the expected result, the observed result and the evidence.
- At least three automated tests, plus those added per increment (section 5). Distinguish manual checks from automated tests, and record what could not be run.
- Review the whole diff with AI support: permissions, validation, error handling, changes outside scope.
- One refactoring opportunity, applied in a separate step, with behaviour checked before and after.
- Update the OpenAPI contract and the documents so that they match the implementation; update `AGENTS.md` with the real run and test commands; complete the README (requirements, installation, configuration, schema, demo data, accounts, start-up, tests).
- Three short examples of AI use in `verification.md` (goal, decision, how it was verified, including a rejected or corrected suggestion if one happened).
- Check that the README steps work from a clean copy; tag the delivered version.

## 5. Automated tests (minimum, and where they come from)

| Test | Level | Increment | Covers |
|---|---|---|---|
| Task and project title validation: valid, too short, too long, whitespace-only, boundary values | Unit | 2 and 4 | NFR-04, EC-01 |
| A Viewer cannot create a task; a non-member gets `404`, also with an invalid body | Integration | 4 | NFR-01, CC-03, CC-05 |
| Create a task, then read it back, against a real PostgreSQL | Integration | 4 | NFR-06, AC-04.4 |
| Login with valid and invalid credentials; no session gives `401` | Integration | 1 | AC-01.1 to AC-01.3 |
| Removing and demoting a member removes assignments | Integration | 5 | AC-03.5, AC-03.8 |
| Progress rounding (1 of 3, 1 of 8, no tasks) | Unit | 6 | AC-06.3 |

Integration tests run against a PostgreSQL container started by the test setup, with the schema from the migrations and data reset between tests. The frontend gets unit tests for its validation and one end-to-end test is left for Ficha 04.

## 6. Order of cuts and risks

**If time runs short, drop in this order:** increment 8 (override), increment 7 (comments), then the pagination and search refinements of increment 6. Everything else is needed for the MVP to make sense. Each cut is recorded in [requirements.md](requirements.md), section 6, and the status of the increment becomes **Cut**.

| Risk | Mitigation |
|---|---|
| The schedule is tight and the scope is at its upper limit. | Increments are ordered by value; the milestone after increment 2 gives a working core early; cuts are decided in advance. |
| A route forgets the access step or the role check. | The access step is built once in increment 2; each increment tests unauthorised access; the final review checks every write route. |
| Cascade and cleanup rules are wrong (assignments, orphaned data). | They are tested at the database level in increments 5 and 6. |
| The generated code does not match the contract. | The contract is the reference; each increment compares the implementation against `openapi.yaml`, and mismatches are fixed on one side, recorded, and never left as is. |
| Docker Desktop is not running. | Documented in the README; the database and the integration tests need it. |

## 7. Definition of done for an increment

1. The acceptance criteria listed for it pass.
2. The checks listed for it have been run, and the results are recorded.
3. The OpenAPI contract and the documents still match what was built, or the difference is recorded.
4. The diff has been reviewed and contains only changes related to the increment.
5. No credentials or real data are in the repository.
6. The status in section 4 is updated, and the work is committed with a clear message.

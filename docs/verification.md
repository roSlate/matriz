# Matriz: Verification

Status: Ficha 03, Exercises 8 and 9. Related documents: [implementation-plan.md](implementation-plan.md), [user-stories.md](user-stories.md), [constraints.md](constraints.md), [architecture.md](architecture.md), [../openapi.yaml](../openapi.yaml).

This document records how the MVP was verified: what was run, what was observed, and what could not be checked. Three kinds of evidence are kept apart, because they prove different things:

| Mark | Kind | What it proves |
|---|---|---|
| **A** | Automated test, run by `mvn verify` or `npm test`. | The behaviour holds on every run, against a real PostgreSQL for the backend. |
| **S** | Scripted check, run once on the final build through the Vite proxy with the seeded users ([verify-api.py](verification/verify-api.py), output in [verify-api-output.txt](verification/verify-api-output.txt)). | The running application, with its real database and a real backend restart, behaves as expected. It is not repeated automatically. |
| **M** | Manual check in a browser, done by the developer (section 5). | What the tests cannot see: the screens, the states, the layout and the style. |

Environment: Linux (WSL2), Java 21.0.12, Maven 3.8.7, Node 24.16, npm 11.13, Spring Boot 4.1.2, React 19, Vite 8, PostgreSQL 17.12 in a container, Docker 29.1. Date of the final run: 2026-11-15.

## 1. Commands run and their results

| Command | Result |
|---|---|
| `cd backend && mvn verify` | **134 tests, 0 failures, 0 errors**; build success in 45 s. Integration tests start a temporary PostgreSQL container. |
| `cd frontend && npm run typecheck` | Clean. |
| `cd frontend && npm test` | **122 tests in 10 files, all passing.** |
| `cd frontend && npm run build` | Success. |
| `python3 docs/verification/verify-api.py 1`, restart of the backend, `python3 docs/verification/verify-api.py 2` | **99 checks, 0 failures.** Database row counts before and after are identical (3 users, 3 projects, 6 memberships, 18 tasks, 4 assignments, 3 comments). |

Backend tests per class: `AuthenticationTest` 10, `CookieAndCsrfFlowTest` 4, `SchemaRulesTest` 8, `ProjectApiTest` 12, `ProjectSearchAndDeleteTest` 10, `ProgressTest` 6, `TaskApiTest` 16, `MemberApiTest` 24, `AssigneeApiTest` 13, `CommentApiTest` 14, `ProgressOverrideApiTest` 10, `StrictJsonTypesTest` 4, `ErrorResponsesTest` 3.

**Mutation checks.** A test that passes proves little unless it fails when the code is wrong. For every security rule added, the rule was disabled for a moment and the tests were run, expecting failures; the code was then restored and compared with the saved copy.

| Increment | Rule disabled | Tests that failed |
|---|---|---|
| 2 | The access step for projects | 4 |
| 3 | The Owner-only check on the three member operations | 3 |
| 4 | The access step for tasks | 8 |
| 5 | Eligibility of an assignee (everyone eligible) | 3 |
| 5 | The role check on assign | 1 |
| 7 | The role check on comments | 2 |
| 7 | Oldest-first order reversed | 2 |
| 8 | The Owner-only check on the override | 1 |
| 8 | `effective` ignoring the manual value | 4 |

## 2. The nine cases of the ficha

Each case lists what was expected, what was observed and the evidence. "S" counts are the checks of the scripted run for that case; every one passed.

| Case | Scenario | Expected | Observed | Evidence |
|---|---|---|---|---|
| 01 | Access to the application | Valid credentials get in; invalid ones and requests without a session are refused in a consistent way. | A wrong password and an unknown email give the same `401` and the same message. A request without a session gives `401` on every route, also unknown ones. Login without the CSRF token gives `403`. Logout invalidates the session. No response contains a password or a hash. | A: `AuthenticationTest`, `CookieAndCsrfFlowTest` (real HTTP, cookies). S: 10 checks. M: items 1 and 2. |
| 02 | Project management | Create, search, edit and delete work according to permissions. | The creator becomes Owner and cannot be supplied by the client. Search finds a project by part of its name and never shows another user's project. Only the Owner edits and deletes; Member and Viewer get `403`. | A: `ProjectApiTest`, `ProjectSearchAndDeleteTest`, `MemberApiTest`. S: 11 checks. M: items 3, 4, 5 and 14. |
| 03 | Persistence of tasks | The task shows without reloading and survives a backend restart. | The task is in the list right after creation, exactly once. After stopping and restarting the backend (Flyway: "schema up to date, no migration necessary") the project, its 3 tasks and the comment are still there. | A: `TaskApiTest.aCreatedTaskIsStoredInTheDatabaseAndCanBeReadBack` and the frontend tests for the task list. S: 6 checks including the real restart. M: items 4 and 6. |
| 04 | Data validation | Empty or whitespace-only required fields and invalid values are rejected. | Empty, whitespace-only, too short, too long, and wrongly typed values (a number or a decimal for text, text for a number) give `400` for projects, tasks, comments, members, assignees and the manual progress. Nothing is saved. | A: `ProjectApiTest`, `TaskApiTest`, `CommentApiTest`, `ProgressOverrideApiTest`, `StrictJsonTypesTest`; frontend form tests. S: 15 checks, with row counts checked. M: items 4, 6, 10 and 13. |
| 05 | Status and priority | An edit updates the task, the filters and the counts coherently. | After a task is set to `DONE`, the counts become 1/1/1 of 3, the computed progress 33 %, the `DONE` filter shows exactly that task, and the status and priority filters combine. A `DONE` task can be reopened. A Viewer cannot edit. | A: `TaskApiTest`, `ProjectApiTest`, `ProgressTest` (rounding); frontend `TasksSection` and task page tests. S: 8 checks. M: item 7. |
| 06 | Assignees | Assigning and removing works; the same assignment is never duplicated. | Several people can be assigned and each removed on its own. The same assignment twice gives `409` and the database holds one row. A Viewer or an outsider cannot be assigned (`422`); a Viewer cannot assign (`403`). | A: `AssigneeApiTest`, `SchemaRulesTest`; frontend `AssigneesPanel` tests. S: 9 checks. M: item 9. |
| 07 | Comments | Content, authenticated author and date are saved and visible. | The author is the session user, the date is set by the server, the text is trimmed. A client-supplied author or date is refused. Readers see the author's name and no email. | A: `CommentApiTest`; frontend `CommentsSection` tests. S: 7 checks. M: item 10. |
| 08 | Authorization in the API | Another user cannot reach or change resources, even by changing identifiers. | Ana, who has no access to "Team handbook", got `404` on 15 different requests for its project and for a task of it, by identifier: read, list, edit, delete, members, progress, tasks, comments, assignees, including an invalid body. Sofia's data was unchanged. A project that does not exist answers the same `404`. | A: the "non-member gets 404, also with an invalid body" tests in every API test class; 9 mutation checks (section 1). S: 20 checks. M: item 12. |
| 09 | Deletion and errors | Deletion respects dependencies; API errors give no success message and no loss of the form's data. | Deleting a task removes its assignment and comment and keeps the other tasks. Deleting a project leaves no task, membership, assignment or comment of it. A deleted project answers `404`, also on a second delete. Error bodies contain no stack trace, class name or SQL. In the interface a failed submission keeps what was typed and shows the error, never a success. | A: `TaskApiTest.deletingATaskRemovesItsAssignmentsAndCommentsButNotOtherTasks`, `ProjectSearchAndDeleteTest`, `SchemaRulesTest`, `ErrorResponsesTest`; every frontend dialog has a "failure keeps the input" test. S: 13 checks. M: items 14 and 15. |

### The three automated tests the ficha requires

| Required test | Test | What it checks |
|---|---|---|
| A validation rule | `TaskApiTest.titleAndDescriptionAreCheckedAfterTrimming` | The title limits (3 to 150) and the description limit (2000) at every boundary, after trimming; whitespace-only is rejected. |
| An authorization scenario | `TaskApiTest.aNonMemberGets404ForEverythingEvenWithAnInvalidBody` and `aViewerCanReadButNotChangeAnything` | An outsider gets `404` for every task route, also with an invalid body; a Viewer reads but gets `403` on every write. |
| A creation followed by a read after persistence | `TaskApiTest.aCreatedTaskIsStoredInTheDatabaseAndCanBeReadBack` | A task is created, found with a direct query in PostgreSQL, and read back through the API. |

## 3. Problems found, investigated and fixed

For each: how it was found, the cause, the limited fix and the regression test.

| # | Problem | How it was found | Cause | Fix and regression test |
|---|---|---|---|---|
| 1 | A cookie test was wrong and a second one passed without testing anything (increment 1). | While writing the authentication tests. | MockMvc does not expose the `Set-Cookie` headers, so assertions on them were wrong or vacuous. | Both tests were replaced by tests over real HTTP: `CookieAndCsrfFlowTest`. |
| 2 | Every anonymous request created a session (increment 1). | During the work on increment 1, recorded in the plan. | Spring Security's saved-request feature. | Disabled in `SecurityConfig`; `CookieAndCsrfFlowTest.anAnonymousRequestGetsACsrfCookieButNoSession`. |
| 3 | Search tests failed on a `%` and `_` (increment 6). | Two new tests failed. | The tests, not the application: MockMvc encodes the URL template, so the text was encoded twice. | The tests pass the text with `.param()`. The application was right. |
| 4 | `{"content":42}` was saved as the text "42", and `"2"` was accepted for a number (found in increment 7). | A test of "a body with anything else is refused" failed on that case. | Jackson converts scalars by default, against the contract (`string`, `integer`). | `allow-coercion-of-scalars: false` fixed only half; a small `StrictJsonConfig` fixed the rest. `StrictJsonTypesTest` failed before and passes after. |
| 5 | A decimal for a whole number was cut short: `80.5` was saved as `80` (increment 8). | A test of the manual progress failed on that case. | Jackson's default for floats read as integers. | `StrictJsonConfig` now refuses it; `ProgressOverrideApiTest` and `StrictJsonTypesTest`. |
| 6 | Unknown routes answered "No static resource…" and a bad query value "Failed to convert…" (found in the review, step 2 of increment 9). | Probing the real API for errors that no controller produces. | The framework's own messages. No stack trace, SQL or path was exposed, but CC-02 asks for fixed texts. | `ApiExceptionHandler` now answers `Resource not found.` and `A query parameter has an invalid value.`; `ErrorResponsesTest` (two of its three tests failed before). |
| 7 | A test about an expired session failed once in a full run (`App.test.tsx`). | One full run of the frontend suite under load. | By reading the code (not reproduced): the "was signed in" flag was set in an effect after the render, so a 401 arriving in that window was not reported as an expired session. | The flag is set when the user is stored (`AuthContext`). The failure never recurred in 10 later runs, but it was not reproduced before either, so this is a reasoned fix, not a proven one. A deterministic regression test is not practical for a window of a few milliseconds. |
| 8 | The first run of the scripted check reported 12 failures where the expected and the observed values were identical. | The output itself. | A bug in the script: a list or tuple was compared with "is in" instead of equality. | Comparator fixed, the data of the flawed run removed, and the whole check rerun from the start (99 checks, 0 failures). |
| 9 | A probe written for the contract comparison renamed a demo project ("Client onboarding" became "zzzz"). | The probe expected `403` for a Member and got `200`. | A mistake in the probe, not in the application: it took the first project of Ana's list, which was a project where Rui is the Owner, so the edit was legitimate. | The name was restored at once (the description, the manual progress and all row counts were untouched; only that project's `updated_at` differs from the seed). No regression test applies, because the application behaved correctly; from then on probes only use throwaway data. |

## 4. Review of the diff, and the refactoring

### Review (Exercise 9.1)

Done with AI support over the whole implementation, with the focus the ficha asks for.

**Checked and found correct**
- **Permissions:** every one of the 13 write routes that need a role calls the role check (13 `requireAtLeast` calls); creating a project is open to any logged-in user by design. Both access-step interceptors cover every route with a project or task identifier. The order of checks is 404, then 400, then 403, and tests assert it for every module.
- **Authentication:** the security configuration is as documented (server session, CSRF cookie with `SameSite=Lax`, `401` for anonymous requests including unknown routes, only the health check exposed).
- **Secrets and data:** no credentials in the repository or in its history (`.env` was never committed); the demo data is fictional and its password is a hash, loaded only by the `dev` profile. Nothing sensitive is logged.
- **Frontend:** no `innerHTML`, no browser storage, no `fetch` outside the API client, no hard-coded backend address except the development proxy, and no mocks left in production code.
- **Scope:** the tracked files contain nothing outside the project's own code, documents and configuration.
- **Contract:** all 23 implemented routes are in `openapi.yaml` with the same methods and paths, and none are missing or extra; the field-level comparison is recorded below.

**Fixed:** problem 6 in section 3.

**Known limits, not fixed (accepted or out of scope)**
- A URL with an empty segment (`/tasks//assignees`) is refused by Tomcat with a plain `400` body, not in the Problem Details format. It reveals nothing.
- If a user is removed from a project between a check and an insert, the database constraint answers a generic `500` and nothing is saved. It only happens when two requests collide.
- There is no login throttling (accepted in constraints.md, section 6) and the session cookie has no `Secure` flag, because HTTPS belongs to the delivery stage.
- There is no limit on the size of a JSON body: a very large body is parsed before it is rejected.

### Contract against implementation (Exercise 9.3)

- **Routes:** all 23 implemented routes are documented with the same methods and paths; none is missing or extra (compared by reading the controllers against `openapi.yaml`).
- **Response bodies:** the real responses of the running API were compared, field by field, with their schemas: the user, a page of projects and one project, a page of tasks and one task, a member, a page of comments and one comment, and the error bodies for `400`, `401` and `404`. No field was missing and none was extra. The `403` body (`code: ROLE_INSUFFICIENT`) is asserted by the API tests.
- **Status codes:** the codes of every operation were compared with what the implementation can return. One gap, found on the contract side: a malformed identifier in the path (for example `/projects/abc`) gives `400`, which is implemented and tested, but 8 operations with a path identifier did not document it. The contract was corrected; the code was right. The generic `405`, `415` and `500` are described once, in the `Problem` schema.
- **New error codes** were added to the `Problem` description: `USER_NOT_FOUND`, `ALREADY_MEMBER`, `OWNER_PROTECTED`, `USER_NOT_ASSIGNABLE` and `ALREADY_ASSIGNED`.
- **Other documents:** `architecture.md` was updated (the comment module's real dependencies, the eligibility lookup, the event as built, strict JSON types, the authorisation tests written earlier than planned). `database.md` needed no change: the schema was built from it and tested against it in increment 0.

### Refactoring (Exercise 9.2)

**Opportunity:** the same expression, `error instanceof ApiError ? error.message : "Something went wrong. Please try again."`, was copied in 11 places (forms, panels and dialogs), so changing the wording needed 11 edits and one could be missed.

**Decision (the developer's):** extract one helper, `errorMessage(error, fallback?)`, next to `ApiError` in `api/client.ts`, with a shared default text; applied in a separate commit, with no change of behaviour. The two places that use their own fallback text keep it. Other candidates were rejected for now: the three repeated lines that read the current user in the two access-step interceptors (smaller benefit) and the nine copies of the "no double submit" reference (a bigger change, riskier this late).

**Verification:** the frontend suite before (119 passed) and after (119 passed, plus 3 new unit tests for the helper: 122 passed, three runs), the type check and the build clean, and no copy of the default text left outside `client.ts`.

## 5. Manual checks in a browser (by the developer)

Done by the developer on 2026-11-15 at http://localhost:5173 with the demo accounts, on the final build (the build that contains every fix of this document). The developer reported that **every item worked**; no per-item notes were given, so none are recorded here. The items are the ones the ficha's cases ask a person to confirm: the screens, the states, the layout and the style.

| # | Check | Case | Result |
|---|---|---|---|
| 1 | Logged out, the site leads to the login page; a wrong password gives "Invalid email or password." and keeps the email; a correct login shows "My projects" with role, counts and progress. | 01 | ✓ |
| 2 | After deleting the `JSESSIONID` cookie, the next click leads to the login page with "Your session has expired…". | 01 | ✓ |
| 3 | Search `web` finds one project; `zzz` shows "No projects match your search" with Clear search. | 02 | ✓ |
| 4 | New project: empty and spaces-only are refused; "QA check project" appears once and survives a reload. | 02, 03, 04 | ✓ |
| 5 | Edit the project name. | 02 | ✓ |
| 6 | New task: `ab` and spaces are refused; "First task" appears without reloading, changes the counts, and survives a reload. | 03, 04 | ✓ |
| 7 | Setting the task to Done updates the counts and progress; the Done filter shows it. | 05 | ✓ |
| 8 | At a narrow width (about 400 px) the pages stay usable, with no horizontal scrolling. | UI | ✓ |
| 9 | Assign Rui and Ana; the select no longer offers assigned people; unassign one; "Unassigned" when none. | 06 | ✓ |
| 10 | Comments: spaces-only refused; a comment shows with name and date and survives a reload; a line break is kept. | 04, 07 | ✓ |
| 11 | Members: add Sofia as Viewer; an unknown email and a duplicate each give a message next to the field. | 02 | ✓ |
| 12 | As Sofia (Viewer) the project can be read but has no New task, Edit or comment form; as Rui (not a member) its address shows "no longer exists or no access". | 08 | ✓ |
| 13 | Manual progress: 40 shows "40% (manual)" with the computed value; `150` and `80.5` are refused; "Use computed value" returns. | 04 | ✓ |
| 14 | Delete project: the dialog states the number of tasks; Cancel keeps it; confirming returns to the dashboard and it is gone. | 02, 09 | ✓ |
| 15 | With the network set to Offline, creating a project shows an error (not a success) and keeps what was typed. | 09 | ✓ |
| 16 | The soft-club style is pleasant and readable everywhere. | UI | ✓ |

## 6. Examples of AI use

**1. A global fix that turned out not to be one line.**
- **Goal:** while writing the comment tests, the assistant found that a number sent as text was saved as text, and proposed leaving the global fix for the final review.
- **Decision (the developer's):** do it now, "one line plus a test plus the restart".
- **Corrected suggestion:** the assistant's "one line" fixed only half of the problem (text for a number); the other half (a number for text) needed a small configuration class. This was found by running the test that failed before, and reported to the developer as a change of size.
- **Verification:** the new tests failed before and pass after; the whole suite (120 at that time) and a real check through the proxy on the restarted backend; the case was put back into the comment tests.

**2. Where the assignment cleanup lives (increment 3).**
- **Goal:** keep the rule "removing or demoting a member removes their assignments" without letting the project module call the task module.
- **Decision (the developer's):** of two options offered, build the task-module listener in increment 3 ("option A") instead of waiting for increment 5, so the rule holds from the start.
- **Verification:** API tests for demotion and removal (assignments gone, task and comments kept, other projects untouched); the same rule checked again through the real members API in the assignee tests; a mutation check on the permission rules of the same operations.

**3. A bug in the verification script, found by its own output (this exercise).**
- **Goal:** a scripted check of the nine cases on the running application.
- **Problem:** the first run printed 12 failures with identical expected and observed values. The assistant diagnosed the comparator, not the application, as the cause, instead of "fixing" the application or hiding the lines.
- **Decision:** fix the comparator, remove the data of the flawed run, and rerun everything from the start so that the recorded evidence is one clean run.
- **Verification:** 99 checks passed, the database counts were identical before and after, and the backend restart was confirmed in its log ("schema up to date").

## 7. What could not be verified

- **HTTPS and the `Secure` cookie flag:** the application was only run over HTTP on localhost.
- **Brute-force protection:** none exists (accepted risk).
- **Other browsers, devices and accessibility tools:** the layout and the style were checked by the developer in one browser at two widths; no automated accessibility audit or screen reader test was run. Known: the dialog does not trap the keyboard focus.
- **Dependency vulnerabilities:** no audit was run (it needs network access).
- **Concurrency and load:** two simultaneous users on the same data, and any performance measure, were not tested.
- **End-to-end tests and CI:** the automated end-to-end test belongs to Ficha 04, and continuous integration to Ficha 05.

## 8. Completion criteria of the ficha

| Criterion | Status | Evidence |
|---|---|---|
| The application starts following the README | Met | The clean-copy check below. |
| The MVP manages projects, tasks, assignees and comments through the interface | Met | Component tests (A), the scripted API run (S) and the 16 manual items (M), sections 2 and 5. |
| Authentication and authorization also work for direct API requests | Met | Case 01 and case 08 (A and S). |
| Data persists after reloading the interface and restarting the backend | Met | Case 03: the real backend restart (S) and the reloads in manual items 4, 6 and 10 (M). |
| The interface handles loading, no data, success and error | Met | Every screen has tests for these states (A); manual items 3, 14 and 15 (M). |
| The contract matches the implementation and the checks have recorded results | Met | Section 4 (contract comparison) and this document. |
| The code was reviewed and the delivered version is identified in Git | Met | Section 4; the annotated tag `ficha-03-delivery` is on the commit that contains this text. |

### Clean-copy check (Exercise 9.6)

A fresh `git clone` of the committed state (commit `89134ce`, with no `.env`, no `node_modules` and no build output) was made in a scratch folder and the README was followed step by step. The scratch copy was deleted afterwards, together with its database and volume.

| Step (as in the README) | Result |
|---|---|
| `cp .env.example .env` and a local password | Done. |
| `docker compose up -d db` | The container became healthy. |
| `mvn -DskipTests package` | The jar was built. |
| Start the backend with `SPRING_PROFILES_ACTIVE=dev` | Flyway migrated the empty schema to version 1000 ("create schema" and "demo data"), the application started, and the health check answered `UP`. The database held the demo data: 3 users, 3 projects, 6 memberships, 18 tasks, 4 assignments, 3 comments. |
| The curl example of the README (login, then `/users/me`) | Logged in as Ana Silva and read the profile. |
| `mvn verify` | 134 tests, 0 failures; build success in 36 s. |
| `npm install`, `npm run typecheck`, `npm test`, `npm run build` | The 101 packages were installed; type check clean; 122 tests passed; the build succeeded. |
| `npm run dev` | The development server served the application (HTTP 200, title "Matriz"). |

**Differences from the README, and what they leave unproven.**
- The database, the backend and the development server ran on other ports (5433, 8082, 5174, through a scratch `compose.override.yaml` that is not part of the repository), so that the check could run next to the developer's own database. The README itself uses the default ports.
- Maven ran with `-o` and npm with `--offline`, so that no network was used. The dependencies came from the local caches; this check therefore does not prove that a first download from the internet works.
- The browser interface was not driven against the clean copy, because the development proxy points at port 8080. The clean copy's frontend was installed, type-checked, tested, built and served, and its backend was checked with curl. The interface itself was checked in section 5, on the same code.


# Matriz: Test plan

Status: Ficha 04, Exercise 1 (with the tests of Exercises 2 to 4). Related documents: [requirements.md](requirements.md), [user-stories.md](user-stories.md), [architecture.md](architecture.md), [../openapi.yaml](../openapi.yaml), [verification.md](verification.md), [quality-security-review.md](quality-security-review.md), [../e2e/README.md](../e2e/README.md).

This plan defines the tests from the **requirements and acceptance criteria**, not from the behaviour of the code. It is written after the MVP, so it also records which scenarios the tests built along the way already cover, and which gaps it closed (the unit test of the validation rules and the end-to-end tests).

## 1. Approach

| Level | What it checks | Tool | Where it runs |
|---|---|---|---|
| **Unit** | One business rule on its own, with no Spring, no database and no network. | JUnit 5 and AssertJ (backend), Vitest (frontend). | `mvn verify`, `npm test`, CI. |
| **Integration** | The API and the real persistence: HTTP status, returned fields, rows in PostgreSQL, permissions. | Spring Boot tests with MockMvc and real HTTP, against a PostgreSQL container started by Testcontainers. | `mvn verify`, CI. |
| **End-to-end** | A path of a person through the interface, in a real browser, against the running application, including the headers and the Content-Security-Policy that nginx sets. | Playwright with Chromium. | `npm test` in `e2e/`, CI. |
| **Scripted API check** | The nine cases of Ficha 03 on the running application, with a real backend restart. | [verify-api.py](verification/verify-api.py). | By hand (development profile). |

**Principles** (from the ficha's guidelines):
- Scenarios come from the requirements; code coverage alone proves nothing about correctness, so it is not used as a goal (no coverage tool is configured).
- Tests can be repeated without depending on earlier data or runs (section 5).
- When a test fails, the first question is which of three it is: a defect of the application, a problem of the test, or a limit of the environment (section 7).

## 2. Scenario matrix: tasks and their permissions

Inputs and expected results follow FR-14 to FR-20, FR-26 and NFR-01 to NFR-04, and the acceptance criteria AC-04, AC-05 and AC-07. Users: Ana is Owner, Rui is Member and Sofia is Viewer of "Website redesign"; Ana has no access to "Team handbook".

| ID | Scenario | Input | Expected result | Requirement | Level | Tests |
|---|---|---|---|---|---|---|
| T-01 | Create a valid task | Title `  Write the brief  ` | `201`; title trimmed; status `TODO` and priority `MEDIUM` by default. | FR-14, AC-04.1 | Integration | `TaskApiTest.aCreatedTaskIsStoredInTheDatabaseAndCanBeReadBack`, `theCreatorMayChooseStatusAndPriority` |
| T-02 | The title rule | Empty, whitespace-only, 2 characters, 151 characters; boundaries 3 and 150; spaces around are not counted; an emoji counts as one character | Invalid: `400` naming the field `title`, nothing saved. Valid at both boundaries. | NFR-04, AC-04.2, AC-04.3, EC-01 | **Unit**, integration, frontend | `FieldValidationTest` (10 tests); `TaskApiTest.titleAndDescriptionAreCheckedAfterTrimming`; `TasksSection.test.tsx`: "refuses a title of only spaces" |
| T-03 | The description limit | 2001 characters | `400`; 2000 is accepted. | FR-14, AC-04.3 | Unit, integration | `FieldValidationTest.anOptionalTextMayBeMissingOrEmptyButNotTooLong`; `TaskApiTest.titleAndDescriptionAreCheckedAfterTrimming` |
| T-04 | Create, then read after persistence | Create a task, then read it | The row exists in PostgreSQL and the API returns the same data; the task is in the list without reloading and after a reload. | NFR-06, AC-04.4 | Integration, **end-to-end** | `TaskApiTest.aCreatedTaskIsStoredInTheDatabaseAndCanBeReadBack`; E2E "a member logs in, opens a project, creates a task and sees it in the list" |
| T-05 | Wrongly typed or unknown input | Unknown field, invalid status or priority, a number for the title | `400`, nothing saved. | S-05, AC-04.7 | Integration | `TaskApiTest.anUnknownFieldOrAnInvalidStatusIsRefused`; `StrictJsonTypesTest` |
| T-06 | A Viewer cannot write | Create, edit, delete as a Viewer | `403`, nothing changes; reading works. | AC-04.5, AC-05.5 | Integration | `TaskApiTest.aViewerCanReadButNotChangeAnything` |
| T-07 | A Member can manage tasks | Create, edit, delete as a Member | Succeeds. | FR-14, FR-16, FR-17 | Integration | `TaskApiTest.aMemberCanCreateEditAndDelete` |
| T-08 | **Access without authorization** | A user with no access reads or changes a task or its project, also with an invalid body | `404` for everything (the existence of the resource is not revealed). | NFR-01, CC-03, CC-05, A-17 | Integration, **end-to-end** | `TaskApiTest.aNonMemberGets404ForEverythingEvenWithAnInvalidBody` (and the same in every API test class); E2E "a user who is not a member of a project cannot open it" |
| T-09 | Changing an identifier | The identifier of a task of another project in the URL | `404`; no data is revealed or changed. | EC-03 | Integration, scripted | Same tests as T-08; scripted case 08 (15 requests) |
| T-10 | Change status and priority | `DONE`, then back to `TODO` | Saved; counts, filters and progress follow; reopening is allowed. | FR-16, AC-05.1, AC-05.2 | Integration | `TaskApiTest.statusAndPriorityChangeAndADoneTaskCanBeReopened`, `projectCountsAndProgressFollowTheTasks` |
| T-11 | Filters | Status, priority, both; a combination with no match | Correct subset; "no match" is an empty page, not an error. | FR-18, AC-09.2 | Integration, frontend | `TaskApiTest.filtersByStatusAndPriorityCombineAndNoMatchIsNotAnError`; `TasksSection.test.tsx` |
| T-12 | An invalid edit changes nothing | Edit with an invalid title | `400`; stored values unchanged. | AC-05.3 | Integration | `TaskApiTest.anInvalidEditChangesNothing` |
| T-13 | Delete keeps the rest | Delete a task | Its assignments and comments go; other tasks stay. | AC-05.4, AC-11.1 | Integration | `TaskApiTest.deletingATaskRemovesItsAssignmentsAndCommentsButNotOtherTasks` |
| T-14 | No session | Any request without a login | `401` in the standard error format, also for unknown routes. | S-01, AC-01.3 | Integration | `AuthenticationTest.withoutASessionTheAnswerIs401InTheProblemFormat`; `ErrorResponsesTest` |
| T-15 | Invalid credentials | A wrong password, an unknown email | The same `401` and the same message; the email is kept in the form. | AC-01.2 | Integration, **end-to-end** | `AuthenticationTest.aWrongPasswordAndAnUnknownEmailGetTheSameAnswer`; E2E "a wrong password is refused with the generic message and keeps the email" |
| T-16 | Security headers on what nginx serves | The home page, a client-side route and a built file, requested through the web service | `X-Frame-Options: DENY`, `nosniff`, `Referrer-Policy`, a Content-Security-Policy with `frame-ancestors 'none'`, no nginx version in `Server`; exactly one copy of the headers on API answers; the interface loads under the policy without violations and the progress bar is drawn. | S-02, CC-02; finding F1 and F2 of [quality-security-review.md](quality-security-review.md) | Pipeline smoke test, **end-to-end** | The "Smoke test through the web service" step of `.github/workflows/ci.yml`; E2E "the pages load under the Content-Security-Policy, without violations, and the progress bar is drawn" |

### The permission matrix as tested

| Operation | Owner | Member | Viewer | No access |
|---|---|---|---|---|
| Read a project, its tasks, members and comments | 200 | 200 | 200 | 404 |
| Create, edit, delete a task; assign; comment | allowed | allowed | 403 | 404 |
| Edit or delete the project; manage members; set the manual progress | allowed | 403 | 403 | 404 |

Every row of this matrix has tests in the API test classes of the module concerned (`ProjectApiTest`, `ProjectSearchAndDeleteTest`, `TaskApiTest`, `MemberApiTest`, `AssigneeApiTest`, `CommentApiTest`, `ProgressOverrideApiTest`). Each of them checks the order of the checks: no access (`404`), then invalid input (`400`), then the role (`403`).

## 3. The scenarios chosen for Ficha 04

| Ficha scenario | Unit | Integration | End-to-end |
|---|---|---|---|
| Invalid title | `FieldValidationTest` | `TaskApiTest.titleAndDescriptionAreCheckedAfterTrimming` | (covered by the lower levels, see below) |
| Create and read a task | | `TaskApiTest.aCreatedTaskIsStoredInTheDatabaseAndCanBeReadBack` | "a member logs in, opens a project, creates a task and sees it in the list": authenticates, opens a project, creates a task, checks it in the list and after a reload, and deletes it |
| Access of a user without authorization | | `TaskApiTest.aNonMemberGets404ForEverythingEvenWithAnInvalidBody` | "a user who is not a member of a project cannot open it" |

The invalid title is tested at the unit and integration levels and not end-to-end, on purpose: an end-to-end test is slow and brittle, and the rule is fully decided by the lower levels, which also run in milliseconds.

## 4. Assumptions the tests encode (checked against the documents)

These are the points where a test written from the code or from habit could have been wrong. Each was taken from the documents.

| Assumption | Source | Consequence for the tests |
|---|---|---|
| A user without access gets `404`, not `403`. | A-17, CC-05, architecture section 5 | Every non-member test expects `404`, also with an invalid body. |
| A Viewer who sends an invalid body gets `400`, not `403`. | The order of checks: `404`, `400`, `403`. | The Viewer tests check that input is validated before the role. |
| Text limits count the **trimmed** value, in **characters**. | A-13, NFR-04 | Boundaries are tested with surrounding spaces and with an emoji. |
| Whitespace-only text is empty. | NFR-04, AC-04.2 | Tested for tabs and line breaks too. |
| The client cannot supply the owner or the author. | NFR-03, S-03, AC-02.3, AC-08.3 | A body with such a field is refused with `400`. |
| A value of the wrong JSON type is refused, not converted. | The contract (`string`, `integer`), S-05 | `StrictJsonTypesTest`. |
| The end-to-end test must not depend on data that earlier runs left. | Ficha 04 guidelines | It creates a task with a unique title and deletes it. |

## 5. Isolation and repeatability

- **Backend:** one PostgreSQL container is started once and shared by all integration test classes. Before every test, all tables are emptied (`TRUNCATE ... RESTART IDENTITY CASCADE`) and the three users are created again, so no test depends on another or on the order.
- **Frontend:** every test replaces the network by a fake backend that answers only the routes it declares, and an unexpected request fails the test.
- **End-to-end:** a unique title per run, and deletion at the end through the interface. The tests only read the demo data otherwise. They ran three times in a row locally, and again in CI.
- **Scripted check:** it ends by comparing the database row counts with the ones it found at the start.
- **Guarding something:** a test that cannot fail proves nothing. Every security rule was disabled for a moment to check that a test fails (the table is in [verification.md](verification.md), section 1), the unit tests of the validation rule were checked in the same way (three deliberate breaks), the end-to-end test of missing access has a negative control (run as a user who has access, it fails), the header checks of the smoke test fail with the old nginx configuration, and the Content-Security-Policy test fails with a policy that blocks scripts.

## 6. Commands and results of the last run

| Command | Result |
|---|---|
| `cd backend && mvn verify` | **144 tests, 0 failures**, of which 10 are the unit tests of `FieldValidationTest` (0.24 s, no database). |
| `cd frontend && npm test` | 122 tests in 10 files, all passing. |
| `cd e2e && npm test` (against `docker compose up -d`) | 4 tests, all passing, in about 5 seconds; repeated runs. |
| The same three, in the pipeline | All succeeded on GitHub: [run 43964025147](https://github.com/roSlate/matriz/actions/runs/43964025147) (144 backend, 122 frontend and 4 end-to-end tests, and the smoke test with the header checks). The end-to-end tests run there against the stack built from the Dockerfiles. |
| A deliberate failure in the pipeline | The run [43957343462](https://github.com/roSlate/matriz/actions/runs/43957343462) failed on the failing test and the Docker job was skipped; after the fix, [run 43957608077](https://github.com/roSlate/matriz/actions/runs/43957608077) succeeded. |

## 7. When a test fails: which kind of problem is it?

| Kind | How to recognise it | Examples from this project |
|---|---|---|
| **Defect of the application** | The test is right, and the behaviour contradicts a requirement. | A number was saved as text (`{"content":42}`); unknown routes answered with the framework's wording. Both found by tests, both fixed with a regression test ([verification.md](verification.md), section 3). |
| **Problem of the test** | The application is right and the test is wrong, or checks nothing. | A cookie test that could never fail; a search test that encoded the text twice; a comparison bug in the verification script that reported 12 false failures. |
| **Limit of the environment** | The failure depends on the machine, not on the code. | Docker Desktop not running after a restart; a port already in use; a timing-sensitive test that failed once under load. |

A test is never made to pass by changing the rule it checks (Ficha 04, Exercise 2): in every case above the rule or the test was corrected on its own merits, and recorded.

## 8. Limits of this plan

- **No coverage measurement.** It is not a goal, but it also means that unexercised code would not be flagged automatically.
- **One browser** (Chromium) and one main end-to-end flow plus two small ones; the other screens are covered by component tests, not by a browser.
- **No automated accessibility, performance, load or concurrency tests.**
- **The end-to-end tests depend on the demo data** of the `dev` profile; they must never run against real data.
- **The scripted check is not repeated automatically**; it is a record of one run on the final build.

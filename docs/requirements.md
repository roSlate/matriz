# Matriz: Requirements

Status: draft for Ficha 01. Related documents: [user-stories.md](user-stories.md), [constraints.md](constraints.md).

## How to read this document

The client provided a single sentence. Everything else is an **assumption** made during requirements analysis, and each assumption is registered in [constraints.md](constraints.md) (IDs `A-xx`). The vision and objectives below are tagged:

- **[Client]**: stated by the client.
- **[Assumed]**: proposed by us, to be confirmed with the client when possible.

The functional requirements (section 3) are not tagged one by one: all of their details (fields, limits, roles, rules) are **[Assumed]**, even where a requirement serves one of the client's three needs, which the "Need" column names.

> Client statement: *"We need an application where our teams can organise projects, manage tasks and track the progress of the work."*

## 1. Vision and objectives

**Vision.** Matriz is a web application where teams keep their projects and tasks in one shared place, so that everyone can see what needs doing, who is doing it and how far along it is. [Client] for organising projects, managing tasks and tracking progress; [Assumed] for the single shared place and for the detail of "who is doing it" (A-01).

**Objectives of the MVP**

| ID | Objective | Origin |
|---|---|---|
| O-1 | Teams can organise work into projects. | [Client] |
| O-2 | Teams can manage tasks inside a project. | [Client] |
| O-3 | Anyone with access can see the progress of the work. | [Client] |
| O-4 | Data is private to the people with access and survives restarts. | [Assumed] |
| O-5 | The app is simple to learn: basic functionality of tools like Trello or Jira, without their configuration complexity. | [Assumed] (A-04) |

## 2. Users and roles

Matriz has one kind of account (a registered user). What a user may do depends on their **role in each project**.

| Role | Description |
|---|---|
| **Owner** | The creator of the project. Exactly one per project. Full control, including members and deletion. |
| **Member** | Works on the project's tasks and comments. |
| **Viewer** | Read-only access to the project. |

Users with no role in a project cannot see it. There is no global administrator and no external or guest access in the MVP. [Assumed] (A-06, A-07)

### Permission matrix

| Action | Owner | Member | Viewer |
|---|:-:|:-:|:-:|
| View the project, its tasks and comments | ✓ | ✓ | ✓ |
| View the project's member list (names and emails) | ✓ | ✓ | ✓ |
| Create and edit tasks; change status and priority; delete tasks | ✓ | ✓ | ✗ |
| Assign and unassign people on tasks | ✓ | ✓ | ✗ |
| Comment on tasks | ✓ | ✓ | ✗ |
| Edit the project; set the progress override | ✓ | ✗ | ✗ |
| Add or remove members; change roles | ✓ | ✗ | ✗ |
| Delete the project | ✓ | ✗ | ✗ |

## 3. Functional requirements

Priority: **M** = Must, **S** = Should (see MVP scope in section 5). The "Need" column names which of the client's three needs a requirement serves; "teams" means it follows from the client's mention of teams, by assumption A-06.

| ID | Requirement | Pri | Need |
|---|---|:-:|---|
| **Access** | | | |
| FR-01 | A user can log in with email and password. | M | teams |
| FR-02 | A user can log out; the session is then invalid. | M | teams |
| FR-03 | A logged-in user can see their own profile (name, email). | M | teams |
| FR-04 | Every operation except login requires authentication. | M | teams |
| **Projects** | | | |
| FR-05 | A user can create a project (name 3–100 characters; description up to 1000, optional). The creator becomes its Owner. | M | organise projects |
| FR-06 | A user can list the projects they have access to, and open one. | M | organise projects |
| FR-07 | The Owner can edit a project's name and description. | M | organise projects |
| FR-08 | The Owner can delete a project after confirming; its tasks, comments, assignments and memberships are deleted with it. | S | organise projects |
| FR-09 | A user can search their accessible projects by name. | S | organise projects |
| **Membership** | | | |
| FR-10 | The Owner can add an existing user to the project with the role Member or Viewer. | M | teams |
| FR-11 | The Owner can change a member's role. If a member becomes a Viewer, their assignments in the project are removed. | M | teams |
| FR-12 | The Owner can remove a member. Their assignments in the project are removed; tasks and comments remain. | M | teams |
| FR-13 | The Owner cannot be removed or demoted (one Owner per project, no transfer in the MVP). | M | teams |
| **Tasks** | | | |
| FR-14 | An Owner or Member can create a task in a project (title 3–150 characters; description up to 2000, optional). Unless the creator chooses other valid values, the status is TODO and the priority MEDIUM. | M | manage tasks |
| FR-15 | A task belongs to exactly one project. | M | manage tasks |
| FR-16 | An Owner or Member can edit a task and change its status (TODO, IN_PROGRESS, DONE; any change allowed, including reopening) and priority (LOW, MEDIUM, HIGH). | M | manage tasks |
| FR-17 | An Owner or Member can delete a task. | M | manage tasks |
| FR-18 | A user can filter a project's tasks by status and by priority, alone or combined. | S | manage tasks |
| **Assignees** | | | |
| FR-19 | An Owner or Member can assign several users to a task, and unassign each. A task may have no assignee. | S | manage tasks |
| FR-20 | Only Owners and Members of the project can be assigned; the same user cannot be assigned twice to a task. | S | manage tasks |
| **Comments** | | | |
| FR-21 | An Owner or Member can add a comment to a task (1–2000 characters, not only whitespace). The author and date are set by the server. | S | teams |
| FR-22 | A user with access can list a task's comments with author and date. Comments remain if the author is later removed from the project. | S | teams |
| **Progress** | | | |
| FR-23 | The dashboard shows the user's accessible projects and, for each, the number of tasks per status, with navigation to the project. | M | track progress |
| FR-24 | Each project shows a computed progress percentage: tasks DONE ÷ total tasks, rounded to the nearest whole number (0 % if there are no tasks). | M | track progress |
| FR-25 | The Owner can set a manual progress percentage (a whole number from 0 to 100) for a project, or clear it to return to the computed value. A manual value is always labelled as such, with the computed value shown alongside. | S | track progress |

## 4. Non-functional requirements

| ID | Category | Requirement |
|---|---|---|
| NFR-01 | Security | Authorisation is enforced on the server for every request, including direct API calls; changing an identifier in a request must not grant access to other users' data. |
| NFR-02 | Security | Passwords are stored only as salted hashes. No real credentials in the repository; demo data is fictional. |
| NFR-03 | Security | The server decides who the project owner and comment author are; clients cannot supply them. |
| NFR-04 | Validation | All input is validated on the server; invalid input is rejected with a clear message and nothing is saved. Limits apply to the trimmed value, counted in characters; whitespace-only values count as empty. |
| NFR-05 | Privacy | Personal data is limited to name and email, shown only to users who share a project. Whether an email is registered can still be learned when adding a member (accepted risk, A-22). |
| NFR-06 | Persistence | Data survives page reloads and backend restarts. |
| NFR-07 | Usability | The UI handles loading, empty, success and error states; keeps typed input when a submission fails; prevents duplicate submissions; works from a minimum screen width of 360 px upwards. |
| NFR-08 | Consistency | Lists, filters and counts update after each create, edit or delete made by the user, without a manual reload. Changes made by other users appear on the next load or navigation (no live updates). Totals reflect the whole data set, not only the visible page. |
| NFR-09 | Testability | At least one automated test each for a validation rule, an authorisation scenario, and create-then-read persistence. |
| NFR-10 | Reproducibility | The application can be started from a clean checkout by following the README. |
| NFR-11 | Capacity | No performance targets were given. The MVP assumes small teams (tens of users, hundreds of projects and thousands of tasks) and sets no measurable response-time requirement. (A-12) |

## 5. MVP scope

**In the MVP**

- **Must:** FR-01 to FR-07, FR-10 to FR-17, FR-23, FR-24.
- **Should:** FR-08, FR-09, FR-18 to FR-22, FR-25.

The MVP is deliberately at the upper limit of what can be delivered. If time runs short, the first items to drop are comments (FR-21, FR-22) and the progress override (FR-25). A dropped story is recorded by moving it to the out-of-scope list below, with the reason; the acceptance checklist in [constraints.md](constraints.md), section 5, then applies to the remaining scope.

Accounts come from seeded demo users. Public registration and password recovery are not part of the MVP.

## 6. Out of scope (initial release)

These exclusions are registered as assumption A-20 in [constraints.md](constraints.md).

- Software-specific features: sprints, backlogs, issue types, code or Git integration.
- Time tracking, budgets, invoicing.
- File attachments.
- Board and calendar views (tasks are shown as lists).
- Task deadlines, subtasks, dependencies, tags.
- Notifications, email, activity history and audit log.
- External or guest users; multiple organisations.
- Import, export and integrations with other systems.
- Public registration, password recovery, two-factor authentication.
- Project archiving, ownership transfer, multiple owners.
- Native mobile apps (a responsive web app is enough).
- GDPR tooling (data export, deletion on request, consent).

## 7. Traceability

| Requirement | User stories |
|---|---|
| FR-01 to FR-04 | US-01 |
| FR-05 to FR-07 | US-02 |
| FR-10 to FR-13 | US-03 |
| FR-14, FR-15 | US-04 |
| FR-16, FR-17 | US-05 |
| FR-23, FR-24 | US-06 |
| FR-19, FR-20 | US-07 |
| FR-21, FR-22 | US-08 |
| FR-09, FR-18 | US-09 |
| FR-25 | US-10 |
| FR-08 | US-11 |

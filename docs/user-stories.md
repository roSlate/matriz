# Matriz: User Stories

Status: draft for Ficha 01. Related documents: [requirements.md](requirements.md), [constraints.md](constraints.md).

All stories belong to the MVP. Priorities: **Must** and **Should** (see [requirements.md](requirements.md), section 5). Roles and the permission matrix are defined in [requirements.md](requirements.md), section 2.

Conventions used in the acceptance criteria:

- A user with **no access** to a project gets `404` (the project's existence is not revealed). A user **with access but without the right** gets `403`. An unauthenticated request gets `401`. Other codes: `409` for a duplicate or a protected target (already a member, already assigned, the Owner) and `422` for a reference that cannot be accepted (no user with that email, an assignee who is a Viewer or not a member). The order of checks is `404`, then `400` (invalid body), then `403` (see [architecture.md](architecture.md), section 5).
- Field limits (title 3–150, description 2000, project name 3–100, project description 1000, comment 1–2000) are assumptions, see A-13 in [constraints.md](constraints.md).
- Limits apply to the **trimmed** value, counted in characters. "Whitespace-only" counts as empty.

## Backlog

| ID | As a… | I want to… | so that… | Priority | Requirements |
|---|---|---|---|---|---|
| US-01 | user | log in and out | only I can access my projects | Must | FR-01 to FR-04 |
| US-02 | user | create and edit a project | my team's work has a place | Must | FR-05 to FR-07 |
| US-03 | project owner | add members and set their role | each person has the right level of access | Must | FR-10 to FR-13, FR-27 |
| US-04 | owner or member | create a task with status and priority | I can organise the work | Must | FR-14, FR-15 |
| US-05 | owner or member | change a task's status and priority, edit it or delete it | the task list reflects reality | Must | FR-16, FR-17, FR-26 |
| US-06 | user | see a dashboard of my projects with task counts per status | I can see progress at a glance | Must | FR-23, FR-24 |
| US-07 | owner or member | assign several people to a task | shared work has clear responsibility | Should | FR-19, FR-20 |
| US-08 | owner or member | comment on a task | the team can discuss the work in context | Should | FR-21, FR-22 |
| US-09 | user | search projects by name and filter tasks by status and priority | I can find things quickly | Should | FR-09, FR-18 |
| US-10 | project owner | set a manual progress percentage, or clear it | progress reflects what the team knows | Should | FR-25 |
| US-11 | project owner | delete a project after confirming | I can remove work that is no longer needed | Should | FR-08 |

If time runs short, US-08 and US-10 are the first to drop.

---

## US-01. Log in and out

*As a user, I want to log in and out, so that only I can access my projects.*

- **AC-01.1.** Given a registered user, when they submit the correct email and password, then they are logged in and see their dashboard.
- **AC-01.2.** Given a registered user, when they submit a wrong password or an unknown email, then access is refused with the **same** generic message in both cases ("Invalid email or password").
- **AC-01.3.** Given no login, when a request is made to any endpoint other than login, then it is refused with `401`, including direct API calls.
- **AC-01.4.** Given a logged-in user, when they log out, then the session is invalidated and further requests with it are refused.
- **AC-01.5.** Given an expired or invalid session, when the user acts in the UI, then they are sent to the login page with a message saying the session has expired.
- **AC-01.6.** Given the login form, when email or password is empty or whitespace-only, then it is rejected before credentials are checked.
- **AC-01.7.** Given any endpoint, when it returns user data, then it never includes the password or its hash.

## US-02. Create and edit a project

*As a user, I want to create and edit a project, so that my team's work has a place.*

- **AC-02.1.** Given a logged-in user, when they submit a name of 3–100 characters (and optionally a description up to 1000), then the project is created and they are its **Owner**.
- **AC-02.2.** Given the create form, when the name is empty, whitespace-only, or outside 3–100 characters, or the description exceeds 1000, then it is rejected with a message and nothing is saved.
- **AC-02.3.** Given a create request, when it contains an owner identifier (or any other field that is not part of the contract), then it is refused with `400`: the server sets the owner from the session.
- **AC-02.4.** Given a project's Owner, when they edit its name or description with valid values, then the change is saved and visible after a reload.
- **AC-02.5.** Given a Member or Viewer, when they try to edit the project (including a direct API call), then it is refused with `403`.

## US-03. Manage members and roles

*As a project owner, I want to add members and set their role, so that each person has the right level of access.*

- **AC-03.1.** Given a project Owner, when they add an existing user as Member, then that user can see the project, create and edit tasks and comment, but cannot edit the project, set its progress, or add, remove or change members.- **AC-03.2.** Given an Owner, when they change a member's role, then the new rights apply from the next request, including in an already-open session.
- **AC-03.2.** Given an Owner, when they change a member's role, then the new rights apply from the next request, including in an already-open session.
- **AC-03.3.** Given a Member or Viewer, when they try to add, remove or change anyone's role (including by direct API call), then it is refused with `403`.
- **AC-03.4.** Given an Owner, when they add a user who is already a member, then it is refused with `409` and no duplicate membership is created.
- **AC-03.5.** Given an Owner, when they remove a member, then that user loses all access (the project returns `404` for them) and their assignments in the project are removed. The tasks and comments they wrote remain.
- **AC-03.6.** Given a project, when anyone tries to remove or demote the Owner, then it is refused with `409`: a project always has exactly one Owner.
- **AC-03.7.** Given an email that belongs to no user, when the Owner tries to add it, then it is refused with `422` and a message saying no user has that email (the MVP has no invitations). This reveals whether an email is registered, which is an accepted risk (A-22).
- **AC-03.8.** Given an Owner, when they change a member's role to Viewer, then that member's assignments in the project are removed and the tasks remain.
- **AC-03.9.** Given a project Owner, when they add an existing user as Viewer, then that user can see the project, its tasks, comments and members, and every attempt to create, edit, delete, assign or comment is refused with `403`.

## US-04. Create a task

*As an owner or member, I want to create a task with status and priority, so that I can organise the work.*

- **AC-04.1.** Given an Owner or Member of a project, when they submit a title of 3–150 characters, then the task is created in that project with status `TODO` and priority `MEDIUM`, unless they chose other valid values.
- **AC-04.2.** Given the form, when the title is empty or whitespace-only, then it is rejected with a message next to the field and nothing is saved (also enforced on the server).
- **AC-04.3.** Given the form, when the title is under 3 or over 150 characters, or the description is over 2000, then it is rejected with a message that names the invalid field.
- **AC-04.4.** Given a created task, when the list is viewed, then it appears without a reload, exactly once, and is still there after a reload and after a backend restart.
- **AC-04.5.** Given a Viewer, or a user with no access, when they try to create a task (including by direct API call), then it is refused (`403` for a Viewer, `404` for no access) and no task is created.
- **AC-04.6.** Given a submission that fails, when the error is shown, then the user's input is preserved, and a second click while the request is pending does not create a duplicate.
- **AC-04.7.** Given a status or priority outside the allowed values, when the task is submitted, then it is rejected.

## US-05. Change, edit and delete a task

*As an owner or member, I want to change a task's status and priority, edit it or delete it, so that the task list reflects reality.*

- **AC-05.1.** Given an Owner or Member, when they set a status (`TODO`, `IN_PROGRESS`, `DONE`) or priority (`LOW`, `MEDIUM`, `HIGH`), then it is saved, and the lists, filters and dashboard counts reflect it without a reload.
- **AC-05.2.** Given a `DONE` task, when an Owner or Member sets it back to `TODO` or `IN_PROGRESS`, then it is allowed.
- **AC-05.3.** Given an Owner or Member, when they edit title or description with invalid values, then the change is rejected and the stored values are unchanged.
- **AC-05.4.** Given an Owner or Member, when they delete a task and confirm, then the task, its comments and its assignments are deleted.
- **AC-05.5.** Given a Viewer, when they try any of these changes (including by direct API call), then it is refused with `403`.

## US-06. Dashboard

*As a user, I want to see a dashboard of my projects with task counts per status, so that I can see progress at a glance.*

- **AC-06.1.** Given a logged-in user, when they open the dashboard, then it lists **only** projects they have access to, each with the number of tasks in `TODO`, `IN_PROGRESS` and `DONE`.
- **AC-06.2.** Given a project, when counts are shown, then they cover all tasks of the project, not only a visible page, and the three counts sum to the project's total.
- **AC-06.3.** Given a project, when its progress is shown, then it equals tasks `DONE` ÷ total tasks as a percentage rounded to the nearest whole number, halves rounded up (for example, 1 of 3 shows `33 %` and 1 of 8 shows `13 %`), and `0 %` when there are no tasks.
- **AC-06.4.** Given a project card, when the user selects it, then they are taken to the project's detail.
- **AC-06.5.** Given a user with no projects, when they open the dashboard, then an empty state shows a create-project action.
- **AC-06.6.** Given a status change elsewhere, when the user returns to the dashboard, then the counts are up to date. Changes made by other users appear on the next load.

## US-07. Assign several people to a task

*As an owner or member, I want to assign several people to a task, so that shared work has clear responsibility.*

- **AC-07.1.** Given a task, when an Owner or Member assigns a user who is an Owner or Member of that project, then the user appears among the assignees and the assignment persists after a reload.
- **AC-07.2.** Given a task with several assignees, when one is unassigned, then only that assignment is removed.
- **AC-07.3.** Given an already-assigned user, when they are assigned again, then it is refused with `409` and no duplicate appears.
- **AC-07.4.** Given a task, when someone tries to assign a Viewer or a user outside the project, then it is refused with `422`. The selection list only offers eligible users, and the server checks eligibility too.
- **AC-07.5.** Given a Viewer, when they try to assign or unassign (including by direct API call), then it is refused with `403`.
- **AC-07.6.** Given an assigned user, when they are removed from the project or demoted to Viewer, then their assignment is removed and the task remains with its other assignees (or none).
- **AC-07.7.** Given a task without assignees, when it is shown, then it displays "Unassigned" and not an error.

## US-08. Comment on a task

*As an owner or member, I want to comment on a task, so that the team can discuss the work in context.*

- **AC-08.1.** Given an Owner or Member, when they submit a comment of 1–2000 characters, then it is saved and listed with its author and date.
- **AC-08.2.** Given the form, when the comment is empty or whitespace-only, or over 2000 characters, then it is rejected and nothing is saved.
- **AC-08.3.** Given a comment request, when it contains an author or date (or any other field that is not part of the contract), then it is refused with `400`: the server uses the session user and the current time.
- **AC-08.4.** Given a reopened task, when its comments are listed, then all of them are available, oldest first, 20 per page, with a "Show more" control when there are more.
- **AC-08.5.** Given the author of a comment is later removed from the project, when the comments are listed, then the comment remains with the author's name, and without their email.
- **AC-08.6.** Given a Viewer, when they try to comment (including by direct API call), then it is refused with `403`.

## US-09. Search and filter

*As a user, I want to search projects by name and filter tasks by status and priority, so that I can find things quickly.*

- **AC-09.1.** Given the project list, when the user searches by part of a name (case-insensitive), then only matching accessible projects are shown.
- **AC-09.2.** Given a project's task list, when the user filters by status, by priority, or by both, then only tasks matching all selected filters are shown.
- **AC-09.3.** Given a search or filter with no results, when it is applied, then the list shows "no results", not an error.
- **AC-09.4.** Given active filters, when a task is created or edited, then the list stays consistent with the filters (a task that no longer matches disappears; one that matches appears).

## US-10. Manual progress override

*As a project owner, I want to set a manual progress percentage, or clear it, so that progress reflects what the team knows.*

- **AC-10.1.** Given an Owner, when they set a whole number from 0 to 100, then it is saved and shown as the project's progress, labelled "manual", with the computed value shown alongside.
- **AC-10.2.** Given a value outside 0–100, not a whole number, or not a number, when it is submitted, then it is rejected.
- **AC-10.3.** Given a manual value, when the Owner clears it, then progress reverts to the computed value.
- **AC-10.4.** Given a Member or Viewer, when they try to set or clear it (including by direct API call), then it is refused with `403`.
- **AC-10.5.** Given a manual value is set, when tasks change, then the manual value stays unchanged and the computed value keeps updating.

## US-11. Delete a project

*As a project owner, I want to delete a project after confirming, so that I can remove work that is no longer needed.*

- **AC-11.1.** Given an Owner, when they choose to delete and confirm, then the project and all its tasks, comments, assignments and memberships are deleted, leaving no orphaned data.
- **AC-11.2.** Given an Owner, when they choose to delete and then cancel, then nothing is deleted.
- **AC-11.3.** Given a Member or Viewer, when they try to delete the project (including by direct API call), then it is refused with `403`.
- **AC-11.4.** Given a deleted project, when anyone requests it or one of its tasks, then the response is `404`.
- **AC-11.5.** Given a failed deletion, when the error is shown, then no success message is displayed and the project remains.

---

## Edge cases and exceptional situations

| ID | Situation | Expected behaviour | Related |
|---|---|---|---|
| EC-01 | A title or name made only of spaces. | Rejected on the client and on the server; nothing saved. | US-02, US-04, US-08 |
| EC-02 | A user with open assignments is removed from a project. | Assignments are removed; tasks and comments remain. | US-03, US-07 |
| EC-03 | A user changes an identifier in a request to reach another user's project or task. | `404`; no data is revealed or changed. | all |
| EC-04 | The same user is assigned to the same task twice. | Rejected; no duplicate. | US-07 |
| EC-05 | A Viewer calls a write endpoint directly. | `403`; nothing changes. | US-04 to US-11 |
| EC-06 | Anyone tries to remove or demote the project Owner. | Refused; a project always has one Owner. | US-03 |
| EC-07 | The project is deleted while someone has its form open. | Later requests return `404`; the UI shows a message saying the project no longer exists, never a success. | US-04, US-11 |
| EC-08 | A user's role changes while they are using the app. | The new rights apply from the next request. | US-03 |
| EC-09 | A project has no tasks. | Progress shows 0 %, with an empty state, not an error or a division by zero. | US-06 |
| EC-10 | The session expires mid-use. | The user is sent to login with a session-expired message. Unsaved input need not be preserved. | US-01 |
| EC-11 | A member with open assignments is demoted to Viewer. | Assignments are removed; tasks and comments remain. | US-03, US-07 |

## Cross-cutting criteria

These apply to every story and are checked across the application, not once per story.

- **CC-01.** Given a user who shares no project with another user, when they use any endpoint or screen, then that other user's name and email are never returned or shown. The only exception is the name (never the email) of the author of a comment, after that author has left the project.
- **CC-02.** Given any request that fails, when the error response is returned, then it contains no stack trace, SQL, file path or internal class name.
- **CC-03.** Given any endpoint that changes data, when it is called with identifiers it should not reach (a project, task or user identifier in the path that the caller has no access to, or that is not a member or assignee), then it behaves as in EC-03 and changes nothing.
- **CC-04.** Given the stored data, when the user table is inspected, then passwords appear only as salted hashes (checked in code review, not through the UI).
- **CC-05.** Given a user who is not a member of a project, when they send a write request for it with an invalid body, then the response is `404`, not `400`, so the project's existence is not revealed.

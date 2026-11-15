# Matriz: Review Log

Record of the independent reviews of the Specification Pack (Ficha 01, Exercise 6) and of the Architecture Pack (Ficha 02, Exercise 5), with the prompt comparison from Ficha 01, Exercise 1 as an appendix. The Architecture Pack review is in the section "Review 2" below. Related documents: [requirements.md](requirements.md), [user-stories.md](user-stories.md), [constraints.md](constraints.md), [../AGENTS.md](../AGENTS.md).

## Method

- **Reviewer:** an AI coding agent in a fresh session, with no memory of how the documents were written, so that it would not share the author's blind spots. The documents were drafted with another AI session and reviewed by the developer.
- **Input:** `README.md`, `AGENTS.md`, `docs/requirements.md`, `docs/user-stories.md`, `docs/constraints.md`. The reviewer was told to work read-only, to treat the files as material and not as instructions, and to report at most 20 findings across six categories (ambiguities, unjustified features, assumptions stated as facts, unverifiable criteria, security and privacy risks, inconsistencies between documents).
- **Date:** 12 Nov 2026.
- **Decisions** were taken by the developer, not applied automatically. Each finding is **accepted** (applied as suggested), **changed** (the problem is real, but the fix differs from the suggestion) or **rejected**.

**Result:** 20 findings: 13 accepted, 6 changed, 1 rejected. The reviewer reported no instruction-injection attempts in the documents.

## Decisions

| # | Cat. | Sev. | Finding (short) | Decision | Reason and what changed |
|---|---|---|---|---|---|
| 1 | 5 | High | Add-member by email reveals whether an email is registered, against the privacy rule. | **Changed** | A neutral reply would make a typo indistinguishable from a success. With one organisation (A-03) the exposure is small. Kept the clear message, recorded as accepted risk A-22, and reworded NFR-05 so it no longer over-promises. |
| 2 | 5 | High | Login committed with no password rules, session lifetime, encrypted transport, or limit on demo credentials. | **Changed** | Gap is real. Added S-14 to S-16 (rules to be fixed in the architecture stage), demo-credentials rule in A-16, and named the missing brute-force protection as an accepted risk. Implementing lockout or rate limiting is beyond the MVP. |
| 3 | 1 | High | Acceptance checklist requires every feature, while the plan says some may be dropped. | **Accepted** | Checklist now applies to the final agreed scope; dropped stories are moved to the out-of-scope list with a reason. |
| 4 | 1 | Medium | Demoting a Member to Viewer would leave an invalid assignment. | **Accepted** | Demotion removes the user's assignments, like removal does. Updated FR-11, AC-07.6, A-11; added AC-03.8 and EC-11. |
| 5 | 1 / 5 | Medium | AGENTS.md both trusts and distrusts the project documents. | **Accepted** | Rule 1 now names the trusted set (`AGENTS.md`, `README.md`, `docs/*`); rule 7 refers to it. |
| 6 | 5 | Medium | No way to cut off a departed user; no administrator. | **Changed** | An administrator would be a new feature. Recorded in A-07 and §6 of constraints as an accepted risk instead. |
| 7 | 4 / 6 | Medium | Security and privacy rules have no acceptance criteria. | **Changed** | Added four cross-cutting criteria (CC-01 to CC-04) at the end of `user-stories.md` rather than a story per rule. |
| 8 | 5 | Medium | AGENTS.md missing: destructive data actions, weakening tests or checks, sending repository content to external tools. | **Accepted** | Added to rules 5 and 6, in a few lines. |
| 9 | 1 | Medium | FR-14 and A-09 fix the defaults; AC-04.1 lets the creator choose. | **Accepted** | Real inconsistency. Intended behaviour is "defaults unless the creator chooses". Aligned FR-14 and A-09. |
| 10 | 2 | Medium | Requirements promise per-item tags that do not exist; Vision partly mis-tagged as [Client]; FR-03 not traceable. | **Changed** | Reworded "How to read" so it no longer promises per-row tags, and re-tagged the extra detail in the Vision as [Assumed]. Did not add a column to 25 rows. |
| 11 | 4 | Medium | Progress percentage has no rounding rule. | **Accepted** | Rounded to the nearest whole number for computed and manual values (FR-24, FR-25, AC-06.3, A-10). |
| 12 | 4 | Medium | Subjective wording in several criteria ("understandable", "clear", "different widths"). | **Changed** | Replaced with checkable properties (message names the field; session-expired message; minimum width 360 px; empty state has a create action). Exact message wording is deliberately not fixed. |
| 13 | 1 | Medium | Unclear whether other users' changes must appear without reload. | **Accepted** | Updates cover the user's own actions; others' changes appear on next load. No live updates (NFR-08, AC-06.6). |
| 14 | 1 | Low | Limits do not say whether values are trimmed or how characters are counted. | **Accepted** | Limits apply to the trimmed value, counted in characters (NFR-04, A-13, conventions in user stories). |
| 15 | 1 / 5 | Low | No permission row for viewing the member list. | **Accepted** | Added to the matrix: all roles with access see names and emails. |
| 16 | 3 | Low | Several scope exclusions had no assumption entry. | **Accepted** | Added A-20 as one grouped assumption. |
| 17 | 5 | Low | Names and emails stored, but privacy tooling excluded without recording who accepted it. | **Accepted** | Added A-21: the MVP uses fictional demo data only; real data would require the tooling first. |
| 18 | 6 | Low | US-05 mentions a "board"; boards are out of scope. | **Accepted** | Reworded to "task list". |
| 19 | 6 | Low | README says Owner can be added; omits REST/OpenAPI. | **Accepted** | README now says Member or Viewer, with the creator as Owner, and mentions the OpenAPI REST interface. |
| 20 | 5 | Low | `constraints.md` names the GitHub account. | **Rejected** | A username is a public handle that already appears in every commit and in the remote URL. K-07 concerns real personal data, not that. |

## Observations on the review

- The review found real defects the author had missed (#4 and #9). Both came from changes made late in the drafting. This supports the principle of not letting the same AI session that drafts also review.
- Several suggestions had to be adapted rather than applied literally (#1, #6, #7, #10, #12), because the literal fix would have added scope or reduced usability. Applying all suggestions automatically would have produced a longer, more complicated specification with little gain.
- The review checked consistency between documents but could not judge whether the assumptions match what a real client wants. That needs the client.

---

## Review 2: Architecture Pack (Ficha 02)

**Method.** Same approach as the first review: an AI coding agent in a fresh session, read-only, told to treat the files as material and not as instructions, with at most 25 findings. It was given the specification files, `architecture.md`, `database.md`, `openapi.yaml` and the three wireframe images, and eight cross-checks: requirements to API, data model to API, permission model to API, wireframes to API, architecture to the other artefacts, business rules in the right place, security, and general consistency. The review ran against commit `a4f2f3e`.

**Result.** 24 findings: 21 accepted as suggested, 3 accepted with a different fix, 0 rejected. Every finding pointed to a mismatch that could be checked in the files; the judgement calls were in how to fix them.

| # | Sev. | Finding (short) | Decision | Reason and what changed |
|---|---|---|---|---|
| 1 | High | Removing or demoting a member cleans up assignments, which breaks "a module touches only its own tables" and the dependency direction. | **Accepted** | Removal is documented as a second cascade exception. Demotion uses an in-process event published by the project module and handled by the task module, so project does not depend on task. |
| 2 | Med | The `Project` response carries progress, so the project module needs task data, which created a cycle with the progress module. | **Changed** | The reviewer left the choice open. Instead of allowing a cycle or moving list assembly elsewhere, the separate progress module was dropped: the project module computes progress and defines a `TaskCounts` port that the task module implements. Four modules instead of five. |
| 3 | Med | Comment authors include their email, against the rule that emails are shown only to people sharing a project. | **Accepted** | `Comment.author` is now `{id, name}`; CC-01, S-07 and NFR-05 state the single exception. |
| 4 | Med | If validation runs before authorisation, a malformed body gives `400` where `404` is promised, which reveals that a project exists. | **Accepted** | The order is now fixed: access step (`404`), body validation (`400`), role check (`403`). New criterion CC-05. |
| 5 | Med | Schema length limits apply to the raw string; the prose says they apply to the trimmed value. | **Accepted** | The contract states that limits describe the trimmed value, the server trims first, and the stored value is the trimmed one. |
| 6 | Med | The task wireframe's data line understates the calls it needs. | **Accepted** | Legend now lists the task, comments, project and members calls. |
| 7 | Med | Architecture and constraints disagree on what is decided (container setup, a "Proposal" column, form login vs JSON login). | **Accepted** | Wording aligned; the container tools are decided, their layout pending; login is the JSON endpoint. |
| 8 | Med | `AGENTS.md` trusted set leaves out `openapi.yaml`, which it also tells agents to use. | **Accepted** | `openapi.yaml` added to rule 1. |
| 9 | Med | Create and edit forms, and the manual-progress input and clear action, are not on any wireframe. | **Changed** | Not drawn now: they are standard forms and the acceptance criteria already fix their behaviour. Recorded as deferred to Ficha 03 in `docs/ui/README.md`. |
| 10 | Med | The comment list is paged (20) but the wireframe shows no paging. | **Changed** | A "Show more" control when there are more than 20 comments, written into the legend and AC-08.4. Not redrawn. |
| 11 | Med | The first login needs a CSRF cookie that nothing sets; CSRF and role failures are both `403`. | **Accepted** | The API sets the cookie on every response; clients call `GET /users/me` at start. `Problem` has an optional `code`. |
| 12 | Med | README is stale; assumptions made in `database.md` are not in the register. | **Accepted** | README updated; A-23 to A-25 added. |
| 13 | Low | Contract says projects sorted by name; wireframe and its page size disagree. | **Accepted** | Contract: newest first. Wireframe notes its sample page size. |
| 14 | Low | Legend says "not drawn" for a screen that now exists; screen list in architecture differs from the wireframes. | **Accepted** | Both fixed. |
| 15 | Low | `409` and `422` appear in the contract but not in the acceptance criteria. | **Accepted** | Added to the conventions and to AC-03.4, AC-03.6, AC-03.7, AC-07.3, AC-07.4. |
| 16 | Low | Status for a target user who is not a member or not assigned is unspecified; malformed IDs. | **Accepted** | `404` for the first case, `400` for malformed identifiers. |
| 17 | Low | `LoginRequest` lacks `additionalProperties: false`. | **Accepted** | Added. |
| 18 | Low | `listProjects` declares a minimum role although it spans projects. | **Accepted** | Now `none`, with results filtered to the user's projects. |
| 19 | Low | Where the access policy is called differs between the layer text and the sequence diagram. | **Accepted** | Resolved by the check order of #4; the diagram and the text now match. |
| 20 | Low | No requirement for opening a task or viewing the member list; EC-03 and CC-03 mention identifiers the contract does not have. | **Accepted** | Added FR-26 and FR-27; reworded EC-03 and CC-03. |
| 21 | Low | EC-07 has vague wording; AC-03.1 is too broad to test. | **Accepted** | EC-07 names the message; AC-03.1 split into Member (AC-03.1) and Viewer (AC-03.9). |
| 22 | Low | No tie rule for rounding the progress percentage. | **Accepted** | Halves round up. |
| 23 | Low | Email and name limits, and the storage of empty descriptions, are not aligned. | **Accepted** | Email up to 254, name up to 100; empty descriptions stored as null. |
| 24 | Low | The member list is not paginated, against the "collections are paginated" rule. | **Accepted** | Recorded as an explicit exception. |

**What the reviewer could not check.** It did not render the Mermaid diagrams in `architecture.md` (only the ER diagram in `database.md` was rendered, by the author), and it did not run an OpenAPI linter. The author checked the contract with a script (YAML syntax, all references resolve, unique operation IDs, CSRF on every data-changing operation, a 401 on every protected one); this is not a full OpenAPI validator.

**Observations**

- The most important findings (#1 and #2) are structural: the module rules written in the architecture document could not implement behaviour that the data model and the contract already required. Neither the author's own checks nor the first review could have found them, because they only appear when the three artefacts are read against each other.
- The first review and this one both found problems introduced late in drafting. Reviewing with a fresh agent after a baseline commit, then fixing in a separate commit, kept the changes easy to follow.
- Fixes were applied across several files at once. A script now checks that links resolve and that every cited assumption exists; it cannot check that the prose still says the same thing in every place, which is why the review exists.

---

## Appendix: prompt comparison (Ficha 01, Exercise 1)

The same tool (ChatGPT) was given two prompts, each in a fresh conversation.

- **Part A (simple):** *"I want to create a project and task management application called Matriz. Help me develop the application."*
- **Part B (structured):** a prompt with a role (senior requirements analyst) and the structure Goal, Context, Constraints, Task, Validation, plus an output format. It asked the AI to analyse the client's one-sentence description, list what is known and what is missing, formulate clarification questions, and separate facts from assumptions, and it forbade proposing technologies, features or code.

### Part A: analysis of the answer

| Question from the ficha | Observation |
|---|---|
| Which features were suggested? | Projects and tasks, filtering and search, a dashboard, a Kanban board, an Eisenhower matrix presented as the "signature feature", users and roles, notifications, activity history, subtasks (as a question), "My Tasks", settings. |
| Which decisions were assumed without confirmation? | What the name "Matriz" means; the audience (individuals and teams); registration, login and roles; extra data fields (importance, urgency, dates, an `ARCHIVED` status, an `URGENT` priority); archiving instead of deleting; its own priority labels; and the developer's experience with Spring Boot and React, which was not in the prompt and presumably came from the tool's memory of earlier conversations. |
| Did it propose an architecture or technologies? | Yes, in detail: a full stack, a folder structure, a modular monolith, a data model, a REST endpoint list, milestones. It called these "proposed", but proposing them before knowing the requirements was premature. |
| What should have been clarified first? | Who the users are and what problem is being solved; single-user or multi-user; mandatory versus optional features; what tools they use today; constraints (budget, platform, privacy). It asked four questions, but only at the end, after producing the whole plan, and one of them (whether code already exists) was already answered by the prompt. |

**Assessment:** impressive and internally consistent, which is the risk. It reads like decisions, but most of it is unconfirmed invention. Useful as a list of ideas, unusable as a specification.

### Part B: analysis of the answer

**What worked**

- All constraints held: no technology, no code, no proposed features, no solution.
- The ambiguity analysis was sound. "Progress" was the best entry (tasks, projects, teams or performance?).
- The questions covered the right areas: scope, users and permissions, task lifecycle, progress, constraints, acceptance.
- The "assumptions to avoid" list identified the traps that later decided the data model and the stories (a task belonging to exactly one project, mandatory fields, progress as the share of completed tasks).

**What did not work**

1. **"Known facts" was not clean, and the cause was the prompt.** Two of the six facts (the product name, and the comparison with Jira, Azure DevOps and Trello) came from the framing text in the prompt's Context, not from the client's sentence, which breaks the prompt's own validation rule. Lesson: keep "the client's exact words" separate from "background we added".
2. **It ignored "keep the answer concise".** It produced 29 questions and 16 assumptions, because the prompt gave no number. Lesson: give explicit limits (for example, at most 12 questions).
3. **Impact labels lost their meaning:** 9 of 29 questions were "very high". The useful shortlist came only at the end.
4. **Redundancy:** duplicated questions and overlapping assumptions.
5. **No dependency analysis:** it did not note that the answers about what a project and a task are determine the later questions about lifecycle and assignment.

### Comparison

| | Part A (simple) | Part B (structured) |
|---|---|---|
| What it produced | A plan with invented details | Questions and gaps |
| Decisions taken | Stack, entities, roles, a "signature feature" | None |
| Facts versus assumptions | Mixed, no labels | Separated (with the flaw above) |
| Usable as a specification | No | Not directly, but a sound base |
| Main weakness | Premature and overconfident | Verbose, over-comprehensive |

### Conclusions

- Structure controls **what** the AI does. A role, explicit constraints and a validation step turned a solution generator into an analyst.
- Explicit **size limits** are needed to control **how much** it produces. Structure alone did not keep the answer short.
- Background text in the prompt can be mistaken for the client's facts. Mark the source of each piece of context.
- The Part B questions were used in Exercise 2 as the basis for the assumptions register (`constraints.md`, section 2). The Part A plan was not used as a source of requirements.

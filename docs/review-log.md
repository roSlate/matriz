# Matriz: Specification Review Log

Record of the critical review of the Specification Pack (Ficha 01, Exercise 6), with the prompt comparison from Exercise 1 as an appendix. Related documents: [requirements.md](requirements.md), [user-stories.md](user-stories.md), [constraints.md](constraints.md), [../AGENTS.md](../AGENTS.md).

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

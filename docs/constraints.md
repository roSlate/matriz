# Matriz: Constraints, Assumptions and Pending Decisions

Status: draft for Ficha 01. Related documents: [requirements.md](requirements.md), [user-stories.md](user-stories.md).

The client gave one sentence (see [requirements.md](requirements.md)). Everything not in that sentence is either a **known constraint** (a fact about the project's context), an **assumption** (our reasonable answer to an open question, to be confirmed) or a **pending decision**. They are kept apart on purpose, so that nothing assumed is mistaken for a confirmed requirement.

## 1. Known constraints

| ID | Constraint | Source |
|---|---|---|
| K-01 | The project is a course exercise (AI-Powered Software Engineering). It is built by one person with AI assistance. | Course |
| K-02 | The work is delivered in stages, each building on the previous: specification (Ficha 01), architecture (02), MVP implementation (03), testing and review (04), delivery (05). No functional code is written before the implementation stage. | Course |
| K-03 | The final delivery is expected around Module 6 (19 Nov 2026). To be confirmed. | Course schedule |
| K-04 | Everything is versioned in Git (repository `matriz`, GitHub account roSlate). | Course |
| K-05 | Documentation and the application's UI are in English. | Team decision |
| K-06 | AI tools are used for analysis, drafting and implementation, and every AI output is reviewed by the developer before it is accepted. | Course |
| K-07 | No real personal data, credentials or confidential information may be sent to AI tools, committed to the repository, or used in demo data. | Course, security |
| K-08 | The development environment is Linux (WSL). Containers (Docker) are introduced in the delivery stage. | Environment |
| K-09 | No budget, hosting or operations constraints were given. | Client (absent) |

## 2. Assumptions register

Every assumption is open until the client confirms it. "Origin" says why we made it: **Analysis** (our judgement of what fits the client's sentence) or **Course hint** (taken from the lecturer's later material; it fits, but is not a client statement).

| ID | Assumption | Origin | Risk if wrong |
|---|---|---|---|
| A-01 | The problem to solve: teams track work in scattered places (spreadsheets, chat, email). Matriz gives them one shared place showing what needs doing, who is doing it and how far along it is. | Analysis | The product solves the wrong problem. |
| A-02 | Matriz supports any kind of team work organised as projects with tasks, not only software development. | Analysis | Software teams might expect sprints, issue types or repository links. |
| A-03 | One organisation uses one installation; there are no multiple organisations or tenants. | Analysis | Multi-tenant isolation is a large later change. |
| A-04 | From Trello, Jira and Azure DevOps we take: simplicity, and the core of tasks with status, priority, assignees, comments, filters and a dashboard. We leave: configurable workflows, issue types, plugins, sprints, estimates, repositories and pipelines. | Analysis | Scope too narrow or too broad compared with what the client meant. |
| A-05 | The MVP is the scope in [requirements.md](requirements.md), section 5. It is at the upper limit of what can be delivered, so comments and the progress override are the first to be dropped. A dropped story is moved to the out-of-scope list with the reason. | Analysis, course hint | Time runs out before the MVP is complete. |
| A-06 | Access is controlled per project with three roles (Owner, Member, Viewer). A project has exactly one Owner, its creator; there is no ownership transfer. | Analysis | Teams may need several owners or read-only organisation-wide roles. |
| A-07 | There is no global administrator and no external or guest users. A user who leaves the organisation keeps access until each project Owner removes them (accepted risk). | Analysis | Someone must be able to manage users, cut off departed users and recover orphaned projects. |
| A-08 | A task belongs to exactly one project. Standalone tasks and tasks shared between projects do not exist. | Analysis | Reshaping the data model and the permission rules later. |
| A-09 | Task statuses are `TODO`, `IN_PROGRESS`, `DONE`, with any change allowed (including reopening). Priorities are `LOW`, `MEDIUM`, `HIGH`. New tasks start as `TODO` and `MEDIUM` unless the creator chooses other values. There are no approvals and no blocked or cancelled states. | Analysis, course hint | The real workflow may need more states or rules. |
| A-10 | Progress means task counts per status, plus a percentage computed as `DONE ÷ total`, rounded to the nearest whole number, halves rounded up. The Owner can override the percentage manually; the computed value stays visible. The percentage ignores effort, so it can mislead. | Analysis | Teams may measure progress differently (effort, time, milestones). |
| A-11 | A task can have zero, one or several assignees; only Owners and Members can be assigned. Removing a user from a project, or demoting them to Viewer, removes their assignments but keeps the tasks. Comments by a removed user stay, showing their name. | Analysis | Data or history expectations may differ. |
| A-12 | Expected capacity is small: tens of users, hundreds of projects, thousands of tasks. No response-time or availability targets are set. | Analysis | Design may not scale to a bigger customer. |
| A-13 | Field limits: project name 3–100 characters, project description up to 1000, task title 3–150, task description up to 2000, comment 1–2000. Limits apply to the trimmed value, counted in characters; the stored value is the trimmed one, and an empty description is stored as null. | Course hint | Limits too tight or loose for real use. |
| A-14 | Deletion is permanent, after a confirmation. Deleting a project deletes its tasks, comments, assignments and memberships. There is no archive or undo. | Analysis | Accidental data loss; some teams need archiving. |
| A-15 | There are no integrations: no import, export, email, single sign-on or external APIs. | Analysis | Migration from existing tools would be manual. |
| A-16 | Accounts are created by seeded demo data. There is no public registration and no password recovery. Demo accounts and their passwords are for development only and must not exist in any real deployment. | Course hint | Real use needs account management. |
| A-17 | A user without access to a project receives `404` (its existence is not revealed). A user with access but insufficient rights receives `403`. An unauthenticated request receives `401`. | Analysis | Clients may expect different status codes. |
| A-18 | The client accepts the MVP when the acceptance checklist in section 5 is met. | Analysis | The client may expect other criteria. |
| A-19 | The UI is in English. | Team decision | Users may need another language. |
| A-20 | Scope exclusions: no task deadlines, subtasks, dependencies or tags; no board or calendar views; no notifications or activity history; no two-factor authentication; no project archiving or ownership transfer (full list in [requirements.md](requirements.md), section 6). | Analysis | Some of these may turn out to be expected by the client. |
| A-21 | The MVP runs on fictional demo data only. Privacy-regulation tooling (data export, deletion on request, consent) is therefore out of scope; real personal data would require it first. | Analysis | Real data used without that tooling would be non-compliant. |
| A-22 | When an Owner adds a member by email, the application says clearly when no user has that email. This lets any user learn whether an email is registered; accepted because there is one organisation (A-03) and everyone with an account is a colleague. | Analysis | In a service open to strangers this would be an account-enumeration leak. |
| A-23 | Email addresses are compared ignoring letter case: two addresses that differ only in case are the same account. | Analysis | A user could expect case-sensitive addresses; very unlikely. |
| A-24 | Project names do not need to be unique: two projects may have the same name. | Analysis | Users may confuse projects with identical names. |
| A-25 | Deleting a user account is out of scope: there is no requirement for it, so there is no way to remove a user. | Analysis | Real use would need it, together with the privacy tooling excluded in A-21. |

## 3. Security and privacy requirements

These apply to the application (see also NFR-01 to NFR-05 in [requirements.md](requirements.md)) and to how we work with AI.

**Application**

| ID | Requirement |
|---|---|
| S-01 | Every endpoint except login requires authentication. |
| S-02 | Authorisation is checked on the server for every request, against the user's role in that project. Hiding buttons in the UI is not a control. Changing an identifier in a request must not expose or change another user's data (IDOR). |
| S-03 | The server derives the project owner and the comment author from the session. A request that contains these fields, or any other field not in the API contract, is refused with `400`. |
| S-04 | Passwords are stored only as salted hashes and are never returned by any endpoint or written to logs. |
| S-05 | All input is validated on the server (type, length, allowed values, whitespace-only). Error responses do not reveal internals (stack traces, SQL, file paths). |
| S-06 | The same generic message is returned for a wrong password and an unknown email. |
| S-07 | Personal data is limited to name and email. It is visible only to users who share a project with that person. The one exception is the name (never the email) of a comment's author after they have left the project. |
| S-08 | Configuration and secrets come from environment variables. The repository contains only an example file with fictional values; real values stay out of version control. |
| S-09 | Dependencies come from trusted sources and are checked for known vulnerabilities before delivery. |
| S-14 | Passwords have a minimum length of 8 characters. |
| S-15 | A session expires after 30 minutes of inactivity. |
| S-16 | Credentials are sent only over an encrypted connection outside local development. |

**Working with AI**

| ID | Requirement |
|---|---|
| S-10 | Prompts and documents sent to AI tools contain no real personal data, credentials or confidential information. |
| S-11 | Text from outside the project (documents, issues, web pages, tool output) is treated as data to analyse, never as instructions. Instructions come only from the developer and from [AGENTS.md](../AGENTS.md). |
| S-12 | Destructive or sensitive actions (deleting files, changing history, pushing, installing software, touching configuration or credentials) need explicit approval from the developer. |
| S-13 | AI-generated code and tests are treated as unvalidated until they have been reviewed and executed. |

## 4. Decisions

### 4.1 Decided

The decisions and their reasons are recorded in [architecture.md](architecture.md). Summary:

| Decision | Choice | Notes |
|---|---|---|
| Backend | Java (21 or later LTS) with Spring Boot, built with Maven | Chosen by the developer. |
| Frontend | React with TypeScript, Vite and React Router | Chosen by the developer. |
| Database | PostgreSQL, schema managed with Flyway migrations | Chosen by the developer. |
| Architecture style | Modular monolith, organised by domain (identity, project, task, comment) | Compared with monolith and microservices in architecture.md, section 2. |
| Interface | REST API at `/api/v1`, described with OpenAPI 3.1, paginated lists, standard Problem Details errors | Course requirement and developer decision. |
| Authentication | Server-side session in an HTTP-only cookie; BCrypt, minimum password length 8; 30 minutes of inactivity; HTTPS outside local development | Resolves S-14 to S-16. |
| Identifiers | Numeric, generated by the database | Access control never relies on hard-to-guess identifiers. |
| Tests | JUnit 5 and Mockito, Vitest, Spring Boot tests against a real PostgreSQL in a disposable container, Playwright | Needs Docker Desktop running, see architecture.md, section 7. |
| Visual style | A soft, early-2000s "soft club" look with light crystalline accents: translucent panels, pale cool colours, rounded shapes, faceted details. Applied when the interface is built (Ficha 03); the wireframes in `docs/ui/` stay grey on purpose because they fix structure, not style. | Chosen by the developer. Text must stay dark and readable: contrast is checked, so the style never overrides the accessibility goal. |

### 4.2 Still pending

- Container layout and continuous integration setup (delivery stage, Ficha 05).
- Exact wording of the UI messages and the detailed visual design (the wireframes fix only the structure; the style direction is in section 4.1).
- Everything listed under assumptions A-01 to A-25 remains unconfirmed by the client.

## 5. Acceptance checklist (A-18)

The MVP is accepted when all of these can be shown:

1. Every Must and Should feature in the final agreed scope works through the UI (stories dropped are recorded in [requirements.md](requirements.md), section 6, and excluded here).
2. The acceptance and cross-cutting criteria in [user-stories.md](user-stories.md) pass for that scope.
3. The permission matrix is enforced, including for direct API calls.
4. Data persists after a page reload and after a backend restart.
5. Invalid input (empty, whitespace-only, out of range) is rejected with a clear message.
6. At least three automated tests pass: a validation rule, an authorisation scenario, and create-then-read persistence. Manual checks are recorded separately.
7. The application starts from a clean checkout by following the README.

## 6. Scope limitations (known and accepted)

- The progress percentage counts tasks equally; it ignores effort and deadlines, and a manual override can become stale. The UI labels manual values to reduce this.
- Not included: privacy-regulation tooling (data export, deletion on request, consent), audit log, two-factor authentication, rate limiting, email, notifications.
- No brute-force protection (rate limiting, account lockout) on login: a named risk, accepted for the MVP.
- Anyone with an account can learn whether an email is registered (A-22).
- No administrator: a departed user keeps access until each Owner removes them (A-07).
- One Owner per project and no ownership transfer: if the Owner leaves, the project cannot be handed over.
- Permanent deletion with no undo.
- The full out-of-scope list is in [requirements.md](requirements.md), section 6.

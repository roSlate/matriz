# Matriz

Matriz is a web application where teams organise projects, manage tasks and follow the progress of their work in one shared place.

It is built as a course project for *AI-Powered Software Engineering*, using AI assistants throughout, with every AI output reviewed by the developer.

## The problem

Teams often keep track of work in scattered places: spreadsheets, chat messages, email. It is hard to see what needs doing, who is doing it and how far along it is. Matriz gives a team one place for that, with the basics of tools like Trello or Jira and none of their configuration complexity.

> The client's request: *"We need an application where our teams can organise projects, manage tasks and track the progress of the work."*

Everything beyond that sentence is an assumption, registered in [docs/constraints.md](docs/constraints.md).

## Goals

- Organise work into projects.
- Manage tasks inside a project, with status, priority, assignees and comments.
- Show progress at a glance.
- Keep data private to the people with access, and keep it safe across restarts.

## Planned MVP features

| Area | What it includes |
|---|---|
| Access | Log in, log out, see who you are. |
| Projects | Create, list, view, edit, delete; search by name. |
| Members | Add and remove members with a role per project (Member or Viewer; the project's creator is its Owner). |
| Tasks | Create, edit, delete; status (`TODO`, `IN_PROGRESS`, `DONE`) and priority (`LOW`, `MEDIUM`, `HIGH`); filter by both. |
| Assignees | Assign several people to a task. |
| Comments | Comment on tasks, with author and date. |
| Progress | Dashboard with task counts per status and a progress percentage per project, with an optional manual override by the project owner. |

What is *not* in the MVP (sprints, notifications, attachments, integrations, and more) is listed in [docs/requirements.md](docs/requirements.md).

## Documentation

| File | Contents |
|---|---|
| [docs/requirements.md](docs/requirements.md) | Vision, users and roles, functional and non-functional requirements, MVP scope, out of scope. |
| [docs/user-stories.md](docs/user-stories.md) | User stories with priorities, acceptance criteria and edge cases. |
| [docs/constraints.md](docs/constraints.md) | Constraints, assumptions, security and privacy requirements, pending technical decisions. |
| [docs/architecture.md](docs/architecture.md) | Architecture decision (modular monolith), technology choices, components, order of access checks. |
| [docs/database.md](docs/database.md) | Data model: entities, relationships, integrity rules and the ER diagram. |
| [openapi.yaml](openapi.yaml) | REST API contract of the MVP (OpenAPI 3.1). |
| [docs/implementation-plan.md](docs/implementation-plan.md) | Increments for building the MVP, with checks, tests, order of cuts and status. |
| [docs/ui/](docs/ui/) | Low-fidelity wireframes: dashboard, project detail, task detail. |
| [docs/review-log.md](docs/review-log.md) | Critical reviews of the specification and of the architecture pack: findings and the decision taken on each, plus the prompt comparison from Exercise 1. |
| [AGENTS.md](AGENTS.md) | Persistent instructions for AI coding agents working on this repository. |

## Technology

Java with Spring Boot (backend), React with TypeScript (frontend), PostgreSQL (database), structured as a modular monolith with a REST API described in OpenAPI. Decisions and reasons are in [docs/architecture.md](docs/architecture.md); open points are in [docs/constraints.md](docs/constraints.md), section 4.2.

## Current status

**Stage: implementation, increments 0, 1, 2, 3, 4, 5 and 6 of 9 done.** The Specification Pack and the Architecture Pack are written and have each been through an independent review. Working now: the skeleton and demo data; **authentication** (session, CSRF, login page); **projects** (a dashboard with role, task counts and progress; searching by name; creating, viewing, editing, and deleting for the Owner); and **tasks** (a task list with status and priority filters and paging, creating, a task page with status and priority changes, editing and deleting), and **members** (a list for everyone in the project; adding by email, changing roles and removing for the Owner), **assignees** (assigning and unassigning people on the task page, only Owners and Members, with the assignments removed when someone is removed or demoted), with access rules enforced on the server for every role. Not built yet: comments and the manual progress override. Progress is tracked in [docs/implementation-plan.md](docs/implementation-plan.md).

Next: comments (increment 7) and the override (8) if time allows, then verification and delivery (9).

## Running the project (development)

Prerequisites: Docker Desktop running (with WSL integration if you use WSL), Java 21, Maven 3.8 or later, Node 20.19 or later.

1. **Configuration.** Copy the example and choose a local database password (letters and digits only). Use the same value for `POSTGRES_PASSWORD` and `DB_PASSWORD`. Set it before the first start, because PostgreSQL only reads it when it creates the volume. `.env` is ignored by Git.

   ```bash
   cp .env.example .env
   ```

2. **Database** (PostgreSQL 17 in a container, listening on 127.0.0.1 only):

   ```bash
   docker compose up -d db
   ```

3. **Backend.** The `dev` profile also loads the demo data. Flyway applies the schema and the demo data once; restarting never duplicates or wipes data.

   ```bash
   cd backend
   mvn -DskipTests package
   set -a && . ../.env && set +a
   SPRING_PROFILES_ACTIVE=dev java -jar target/matriz-backend-0.1.0-SNAPSHOT.jar
   ```

   Health check: `curl http://localhost:8080/api/v1/actuator/health` returns `{"status":"UP",...}`.

4. **Frontend** (in another terminal):

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

   Open http://localhost:5173 and log in with one of the demo accounts below. The dev server forwards `/api` to the backend on port 8080.

**Tests.** Backend: `cd backend && mvn verify` (starts a temporary PostgreSQL container, so Docker must be running; the first run downloads the image and takes a few minutes). Frontend: `cd frontend && npm test`; type check with `npm run typecheck`.

**Stopping.** `docker compose down` stops the database and keeps its data. `docker compose down -v` also deletes the data (the demo data comes back on the next start of the backend with the `dev` profile).

**Demo accounts** (fictional, development only, loaded by the `dev` profile; all use the password `matriz-demo`):

| User | Email | Website redesign | Client onboarding | Team handbook |
|---|---|---|---|---|
| Ana Silva | ana@example.org | Owner | Member | no access |
| Rui Costa | rui@example.org | Member | Owner | no access |
| Sofia Lima | sofia@example.org | Viewer | no access | Owner |

Never activate the `dev` profile in a real deployment.

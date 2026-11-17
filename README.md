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

## MVP features

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
| [docs/verification.md](docs/verification.md) | The nine verification cases with expected result, observed result and evidence; automated, scripted and manual checks kept apart; problems found; review; what could not be verified. The scripted check is in [docs/verification/](docs/verification/). |
| [docs/test-plan.md](docs/test-plan.md) | The test scenarios derived from the requirements, the permission matrix as tested, which test covers what, and the results of the last run. |
| [docs/quality-security-review.md](docs/quality-security-review.md) | Code and security review: findings with evidence and decisions, the two-account authorization review, the static analysis (Semgrep). |
| [docs/final-report.md](docs/final-report.md) | The final report: changes, results of the tests and the pipeline, problems and solutions, use of AI agents, limitations, and the final validation from a fresh clone. |
| [CHANGELOG.md](CHANGELOG.md), [docs/release-notes-1.0.0-rc.1.md](docs/release-notes-1.0.0-rc.1.md) | What changed, and the notes of the release candidate `1.0.0-rc.1`. |
| [e2e/README.md](e2e/README.md) | The end-to-end tests (Playwright). |
| [docs/review-log.md](docs/review-log.md) | Critical reviews of the specification and of the architecture pack: findings and the decision taken on each, plus the prompt comparison from Exercise 1. |
| [AGENTS.md](AGENTS.md) | Persistent instructions for AI coding agents working on this repository. |

## Technology

Java with Spring Boot (backend), React with TypeScript and Vite (frontend), PostgreSQL (database, schema by Flyway), structured as a modular monolith with a REST API described in OpenAPI. Docker and Docker Compose run the whole application (nginx serves the frontend and forwards `/api` to the backend), GitHub Actions runs the tests and builds the images, and Playwright drives the end-to-end tests. Decisions and reasons are in [docs/architecture.md](docs/architecture.md); open points are in [docs/constraints.md](docs/constraints.md), section 4.2.

## Current status

**Release candidate `1.0.0-rc.1`: the MVP is implemented, verified, containerised and covered by a pipeline.** All nine increments of [docs/implementation-plan.md](docs/implementation-plan.md) are done:

| Area | State |
|---|---|
| Access | Login, logout, session, CSRF protection, login page, session-expired message. |
| Projects | Dashboard with role, task counts and progress; search by name; create, view, edit; delete (Owner). |
| Members | Add by email, change role, remove (Owner); the list is visible to every member. |
| Tasks | List with status and priority filters and paging; create, edit, change status and priority, delete. |
| Assignees | Assign and unassign Owners and Members; assignments are removed when someone is removed or demoted. |
| Comments | Oldest first with "Show more"; written by Owners and Members; shown with the author's name only. |
| Progress | Computed from the tasks; the Owner can set a manual value (0 to 100) or go back to the computed one. |
| Security | Every rule is enforced on the server, also for direct API calls; a user without access gets `404`; security headers and a strict Content-Security-Policy on the pages. |
| Delivery | Docker images and `docker compose up -d --build`; a GitHub Actions pipeline (tests, then images, smoke test and end-to-end tests); a changelog and release notes. |

How it was checked, what was found and what could not be verified are in [docs/verification.md](docs/verification.md), [docs/test-plan.md](docs/test-plan.md) and [docs/quality-security-review.md](docs/quality-security-review.md).

## Running the project (development)

### Quick start with Docker

The shortest way to run Matriz. You need only **Git** and **Docker Desktop (running)**: no Java, Maven or Node. The repository is private, so you need access to clone it.

**1. Get the code**

```bash
git clone https://github.com/roSlate/matriz.git
cd matriz
```

**2. Create your configuration**

```bash
cp .env.example .env
```

Open `.env` in a text editor and replace the two passwords `change-me-locally` (`POSTGRES_PASSWORD` and `DB_PASSWORD`) with **the same password of your own**, letters and digits only. On Linux, macOS or WSL one command does it (replace `MyPassword123` with your password; on macOS write `sed -i ''`):

```bash
sed -i 's/change-me-locally/MyPassword123/g' .env
```

Leave the rest of the file as it is: it already turns on the demo data and uses port 8088.

**3. Start everything**

```bash
docker compose up -d --build
```

The first time takes several minutes, because the base images and the dependencies are downloaded. Later starts take well under a minute.

**4. Check that it is ready**

```bash
docker compose ps
```

You should see three services, `db`, `backend` and `web`, each with the status `healthy`. If one says `starting`, wait a minute and run the command again.

**5. Open it**

Go to **http://localhost:8088** and log in with `ana@example.org` and the password `matriz-demo`. The other demo accounts are listed [below](#database-schema-and-demo-data).

**Stop and start again.** `docker compose down` stops it and keeps the data; `docker compose up -d` starts it again. `docker compose down -v` also deletes the data, so the next start begins from the clean demo data.

**If something goes wrong**

| What you see | Why | What to do |
|---|---|---|
| `docker: command not found`, or "Cannot connect to the Docker daemon" | Docker Desktop is not running (or, on WSL, its WSL integration is off). | Start Docker Desktop and wait until it says it is running; on WSL, turn on the integration for your distribution in its settings. |
| "Set POSTGRES_PASSWORD in .env" | There is no `.env` file. | Run `cp .env.example .env` (step 2). |
| "port is already allocated" for 8088 | Another program uses that port. | Put `WEB_PORT=8090` (or any free port) in `.env`, start again, and open that port instead. |
| The page opens, but the login says "Invalid email or password" | The demo data was not loaded, because `SPRING_PROFILES_ACTIVE=dev` is missing from your `.env` (for example an older `.env`). | Add the line `SPRING_PROFILES_ACTIVE=dev` to `.env`, then run `docker compose down -v` and `docker compose up -d --build`. |
| "password authentication failed" in the backend logs | PostgreSQL reads the password only when it first creates its data, and you changed it afterwards. | Run `docker compose down -v` (this deletes the data) and start again. |
| A service stays `unhealthy` | It failed to start. | Run `docker compose logs --tail=50` to see why. |

The rest of this section explains each part, and how to run the backend and the frontend on your own machine to work on the code.

### Requirements

- Docker Desktop running (with WSL integration if you use WSL). This is all the quick start above needs.
- To run the backend and the frontend **on your machine** (steps 2 and 3 below) or to run their tests: Java 21 and Maven 3.8 or later, and Node 20.19 or later (with npm).
- Optional: Python 3, only for the scripted check in [docs/verification/](docs/verification/).

### Configuration

Copy the example and choose a local database password (letters and digits only). Use the same value for `POSTGRES_PASSWORD` and `DB_PASSWORD`. Set it before the first start, because PostgreSQL only reads it when it creates the volume. `.env` is ignored by Git; only `.env.example`, with fictional values, is versioned.

```bash
cp .env.example .env
```

| Variable | Used by | Meaning |
|---|---|---|
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | `compose.yaml` | Name, user and password of the PostgreSQL 17 container. The password has no default: the container does not start without it. |
| `DB_URL` | backend | JDBC URL of the database (default `jdbc:postgresql://localhost:5432/matriz`). |
| `DB_USER`, `DB_PASSWORD` | backend | Database user (default `matriz`) and password (no default). |
| `SPRING_PROFILES_ACTIVE` | backend, `compose.yaml` | Set to `dev` to also load the demo data. Never in a real deployment. |
| `WEB_PORT` | `compose.yaml` | Host port of the web service when the whole application runs in Docker (default `8088`). |

### Start-up

1. **Database** (PostgreSQL 17 in a container, listening on 127.0.0.1 only; the data lives in a named volume):

   ```bash
   docker compose up -d db
   ```

2. **Backend.** The `dev` profile also loads the demo data. Flyway creates the schema and loads the demo data once; restarting never duplicates or wipes data.

   ```bash
   cd backend
   mvn -DskipTests package
   set -a && . ../.env && set +a
   SPRING_PROFILES_ACTIVE=dev java -jar target/matriz-backend-1.0.0-rc.1.jar
   ```

   Health check: `curl http://localhost:8080/api/v1/actuator/health` returns `{"status":"UP",...}`.

3. **Frontend** (in another terminal):

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

   Open http://localhost:5173 and log in with one of the demo accounts below. The dev server forwards `/api` to the backend on port 8080, so the browser sees one address and no cross-origin rules are needed.

### Whole application with Docker Compose

Instead of steps 2 and 3 above, one command starts the database, the backend and the web server in containers:

```bash
docker compose up -d --build
```

- Open http://localhost:8088 and log in with one of the demo accounts below. `docker compose ps` shows the three services as `healthy`; `docker compose logs --tail=50` shows their logs.
- The first build takes several minutes, because the base images and the Maven and npm dependencies are downloaded; later builds reuse them.
- Only the `web` service (nginx) is published, on `127.0.0.1:8088` (change it with `WEB_PORT` in `.env`). nginx serves the frontend and forwards `/api` to the backend, so the browser sees one address. The backend is not reachable from the host.
- `SPRING_PROFILES_ACTIVE=dev` in `.env` loads the fictional demo data. Leave it empty to start without demo data, and never use `dev` in a real deployment. **If your `.env` was created before the Docker files existed, add the two new lines (`WEB_PORT` and `SPRING_PROFILES_ACTIVE`) from `.env.example`**; without `dev` the schema is created but there are no demo users, so no login works.
- `docker compose up -d db` still starts only the database, for working on the code with the backend and the frontend on the host.
- The data lives in the named volume `matriz_db`: `docker compose down` removes the containers and keeps the data, and `docker compose down -v` also deletes it.

### Database schema and demo data

- **Schema.** The six tables (users, projects, project members, tasks, task assignments, comments), their constraints and indexes are created by the Flyway migration `backend/src/main/resources/db/migration/V1__create_schema.sql`, written from [docs/database.md](docs/database.md). It runs automatically when the backend starts on an empty database, and not again.
- **Demo data.** `backend/src/main/resources/db/demo/V1000__demo_data.sql` is a separate Flyway location, enabled only by the `dev` profile: three fictional users, three projects with different access for each, and a few tasks, assignments and comments. It runs once.
- **Starting again from scratch.** `docker compose down -v` deletes the database volume; the next start of the backend recreates the schema and the demo data.

**Demo accounts** (fictional, development only; all use the password `matriz-demo`, stored as a BCrypt hash):

| User | Email | Website redesign | Client onboarding | Team handbook |
|---|---|---|---|---|
| Ana Silva | ana@example.org | Owner | Member | no access |
| Rui Costa | rui@example.org | Member | Owner | no access |
| Sofia Lima | sofia@example.org | Viewer | no access | Owner |

### Tests

| What | Command |
|---|---|
| Backend (unit and integration tests against a temporary PostgreSQL container; the first run downloads the image and takes a few minutes) | `cd backend && mvn verify` |
| Frontend tests, type check and production build | `cd frontend && npm test`, `npm run typecheck`, `npm run build` |
| End-to-end tests in a real browser, against the whole application running (for example `docker compose up -d --build`); see [e2e/README.md](e2e/README.md) | `cd e2e && npm install && npx playwright install chromium && npm test` |
| Scripted check of the nine cases of the ficha against the running application (dev profile; creates and removes throwaway data) | `python3 docs/verification/verify-api.py 1`, restart the backend, `python3 docs/verification/verify-api.py 2` |

The results of the last full run are in [docs/verification.md](docs/verification.md).

### Stopping

`docker compose down` stops the database and keeps its data. `docker compose down -v` also deletes the data.

## Continuous integration

The workflow [.github/workflows/ci.yml](.github/workflows/ci.yml) runs on every push and pull request, with read-only permissions and no secrets:

1. **Backend:** `mvn verify` (the integration tests start a temporary PostgreSQL container).
2. **Frontend:** type check, tests and production build.
3. **Docker**, only when 1 and 2 pass: it validates `compose.yaml`, builds the images, starts the stack and waits until every service is healthy, runs a smoke test through nginx (the page, the security headers, a login as a demo user), and then the end-to-end tests. A failing test fails the pipeline and no image is built. The results are in the Actions tab of the GitHub repository.

## Authentication and the API

The API is described in [openapi.yaml](openapi.yaml) (OpenAPI 3.1) and lives under `/api/v1`.

- **Login.** `POST /api/v1/auth/login` with `{"email": "...", "password": "..."}`. A wrong password and an unknown email give the same `401` and the same message. On success the server starts a session and answers with the user.
- **Session.** A server-side session in the `JSESSIONID` cookie (HTTP-only, `SameSite=Lax`). It ends after 30 minutes without activity or on `POST /api/v1/auth/logout`. Every route except login and the health check answers `401` without a session. `GET /api/v1/users/me` returns the signed-in user.
- **CSRF protection.** The server sets an `XSRF-TOKEN` cookie that scripts can read. Requests that change data (`POST`, `PUT`, `PATCH`, `DELETE`) must send its value in the `X-XSRF-TOKEN` header, otherwise they get `403`. The frontend does this in one place, `frontend/src/api/client.ts`.
- **Errors.** Problem Details (RFC 9457), with a `code` when the status alone is ambiguous and an `errors` list for invalid fields. A user with no access to a project gets `404`, the same as for a project that does not exist; a user with access but a role that is too low gets `403`.

```bash
# Example: log in and read the profile (the CSRF token comes from the first response's cookie)
curl -c jar -s -o /dev/null http://localhost:8080/api/v1/users/me
TOKEN=$(awk '$6=="XSRF-TOKEN"{print $7}' jar)
curl -s -b jar -c jar -X POST http://localhost:8080/api/v1/auth/login \
  -H "X-XSRF-TOKEN: $TOKEN" -H "Content-Type: application/json" \
  -d '{"email":"ana@example.org","password":"matriz-demo"}'
curl -s -b jar http://localhost:8080/api/v1/users/me
```

## Repository layout

```
matriz/
├── README.md, AGENTS.md, CHANGELOG.md, openapi.yaml, compose.yaml, .env.example
├── .github/workflows/    the CI pipeline
├── docs/                 specification, design, implementation plan, verification, test plan, review, release notes
│   └── verification/     the scripted check and its recorded output
├── backend/              Spring Boot (Maven): identity, project, task, comment, security, common; Dockerfile
│   └── src/main/resources/db/{migration,demo}
├── frontend/             React and TypeScript (Vite): api, components/ui, features, pages; Dockerfile, nginx.conf
└── e2e/                  end-to-end tests (Playwright)
```

# Matriz: Final report

Status: Ficha 05, delivery and Exercise 6. Version `1.0.0-rc.1` (release candidate), Git tag `v1.0.0-rc.1`. Date: 2026-11-16. Related documents: [../README.md](../README.md), [../CHANGELOG.md](../CHANGELOG.md), [release-notes-1.0.0-rc.1.md](release-notes-1.0.0-rc.1.md), [verification.md](verification.md), [test-plan.md](test-plan.md), [quality-security-review.md](quality-security-review.md), [implementation-plan.md](implementation-plan.md).

The report follows the five points the ficha asks for (sections 1 to 5), then the final validation of Exercise 6 (section 6) and the state of the optional challenges (section 7).

## 1. Changes implemented

The project was built in stages, each in the same repository and each documented:

| Stage | What was delivered |
|---|---|
| Specification (Ficha 01) | Requirements, user stories with acceptance criteria, constraints and assumptions, `AGENTS.md`. |
| Architecture (Ficha 02) | Modular monolith, data model with an ER diagram, the OpenAPI contract, wireframes. |
| Implementation (Ficha 03) | The MVP in nine increments: access, projects, members and roles, tasks, assignees, comments, manual progress, with server-side authorization everywhere; verification and review. Tag `ficha-03-delivery`. |
| Tests and delivery (Fichas 04 and 05) | See below. |

**Changes of the last stage** (after `ficha-03-delivery`):

- **Docker:** a multi-stage image for the backend (Maven build, then a slim Java 21 runtime, non-root) and for the frontend (Node build, then unprivileged nginx that serves the application and forwards `/api`); `.dockerignore` files; `compose.yaml` with the database, the backend and the web service, healthchecks, ordered start-up and a persistent volume; new variables in `.env.example`.
- **Continuous integration:** the GitHub Actions workflow `.github/workflows/ci.yml` (section 3).
- **Tests:** a unit test of the validation rules (10 tests) and end-to-end tests with Playwright (4 tests), plus the test plan.
- **Security review:** security headers and a strict Content-Security-Policy on the pages served by nginx, and no version in the `Server` header (findings F1 and F2); a static analysis with Semgrep.
- **Quality of the code:** a small refactoring (one `errorMessage` helper replacing 11 copies), a fix of a timing race in the frontend, and fixed texts for unknown routes and bad query values.
- **Documents:** test plan, quality and security review, changelog, release notes, this report, and the README and `AGENTS.md` brought up to date.
- **Version:** `1.0.0-rc.1`.

## 2. Results of the tests and the pipeline

| Check | Result |
|---|---|
| Backend (`mvn verify`, real PostgreSQL through Testcontainers) | **144 tests, 0 failures** |
| Frontend (`npm test`, type check, build) | **122 tests**, clean |
| End-to-end (Playwright, Chromium) | **4 tests**, all passing |
| Scripted check of the nine cases of Ficha 03, with a real backend restart | 99 of 99 |
| Manual check in a browser by the developer | 16 of 16 |
| Static analysis (Semgrep, 677 rules, 164 files) | 5 findings, none confirmed ([quality-security-review.md](quality-security-review.md)) |

**The pipeline on GitHub** ([.github/workflows/ci.yml](../.github/workflows/ci.yml)): it runs on every push and pull request, with read-only permissions, no secrets (the only password is a throwaway created inside the run), the runner image and every action pinned. The `backend` and `frontend` jobs run first; the `docker` job needs both, then validates `compose.yaml`, builds the images, starts the stack and waits until every service is healthy, runs a smoke test through nginx and the end-to-end tests.

| Run | What it shows | Result |
|---|---|---|
| [43955204929](https://github.com/roSlate/matriz/actions/runs/43955204929) | First run on `main`: frontend 35 s, backend 1 min 19 s, Docker 1 min 59 s. | success |
| [43957343462](https://github.com/roSlate/matriz/actions/runs/43957343462) | **Controlled failure** (Exercise 4): a test that fails on purpose, on a temporary branch. The backend job failed, the frontend job passed, and the **Docker job was skipped**, so no image is built when a required test fails. | failure, as intended |
| [43957608077](https://github.com/roSlate/matriz/actions/runs/43957608077) | The same branch after removing the failing test. | success |
| [43959360867](https://github.com/roSlate/matriz/actions/runs/43959360867) | The end-to-end tests added to the pipeline. | success |
| [43964025147](https://github.com/roSlate/matriz/actions/runs/43964025147) | The security-header checks added to the smoke test. | success |
| [43967409485](https://github.com/roSlate/matriz/actions/runs/43967409485) | The final code before the tag (`9ab45f6`): 144 backend, 122 frontend and 4 end-to-end tests, and the Docker job. | success |

## 3. Problems found and solutions

The problems of the implementation stage (nine, with their regression tests) are in [verification.md](verification.md), section 3. In the last stage:

| Problem | How it was found | Solution |
|---|---|---|
| The pages served by nginx had no security headers, and the `Server` header showed the nginx version. | The review of the new nginx configuration, probed with `curl`. | Headers and a strict policy in `frontend/security-headers.conf`, `server_tokens off`; checks in the smoke test and an end-to-end test, each proven to fail without the fix. |
| A first fix allowed inline style attributes in the policy. | A negative control run while testing the fix. | The control showed the exception was unnecessary (React sets styles through the DOM, which a policy does not block); the final policy is stricter. |
| The first smoke test of the stack failed at the login. | Running the pipeline's commands against the local stack. | Not a defect: the developer's older `.env` lacked the new `SPRING_PROFILES_ACTIVE=dev`, so no demo users were loaded. The README now says so in bold, and in CI `.env` comes from `.env.example`. |
| An end-to-end test of missing access passed without proof that it could fail. | Writing the test. | A negative control: the same test run as a user who does have access fails. |
| Semgrep reported five findings. | The static analysis. | Triaged: four false positives (the fictional demo password and its hash) and one suspicion (the `Host` header) checked and not confirmed. The demo migration was not edited, to avoid changing its checksum. |
| A probe I wrote for the contract comparison renamed a demo project. | The unexpected `200` status of the probe. | Restored at once; probes now use throwaway data only ([verification.md](verification.md), problem 9). |
| Heavy downloads: the first image build took almost 15 minutes and the Semgrep image about 20. | Observed. | Accepted; they happen once, and the README warns about the first build. |

## 4. Use of AI agents

**How the agent was used.** The development was done with an AI coding agent (Claude Code, model Claude Sonnet 5.5) under the rules of [AGENTS.md](../AGENTS.md), which the agent read first. The working method was the same for every increment and step:

1. The agent analysed the relevant documents and code and proposed a **plan** (what changes, which files, how it is verified). The developer approved, changed or rejected it.
2. The agent implemented **one increment**, then ran the real checks and showed their results, including mutation checks that disable a security rule on purpose to prove that a test fails.
3. The agent reported what was changed, assumed and **not verified**, and **asked before every commit, push, tag or download** from the network. The developer approved each batch.

**Decisions that were the developer's**, among them: building the assignment cleanup in the membership increment (option A) and not later; how a person's own comment is shown after posting; fixing the global JSON strictness at once; which refactoring to apply (the error-message helper); the tag name for the delivery; the release candidate version `1.0.0-rc.1`; running the manual browser checks themselves; approving the Chromium and Semgrep downloads; and logging in to GitHub so that the agent could read the pipeline results.

**How the results were checked.** Not by trusting the agent: by tests that fail when the code is wrong (negative controls and mutation checks), by running the real application (curl through the proxy, a real backend restart, a fresh clone), by the developer's own manual check in a browser, and by reading the agent's evidence, not its claims.

**Where the agent was wrong, and how it was caught** (all recorded in the documents): a cookie test that could never fail; a search test that encoded the text twice; a "one line" fix that fixed only half of a problem; a comparison bug in the verification script that reported 12 false failures; a probe that renamed a demo project; and a proposal to allow inline styles that the agent's own control run disproved. Each was found by looking at real output, and corrected in the open.

**Limit of the method.** The agent writes the code, the tests and the documents; the developer reviews and decides. A reviewer who does not read what the agent produces would not be protected by these controls. For this reason the developer approved the plans and the commits, and ran the checks that need a person.

## 5. Limitations and pending items

Known and accepted (details in [release-notes-1.0.0-rc.1.md](release-notes-1.0.0-rc.1.md), section 5, and [verification.md](verification.md), section 7):

- No HTTPS and no `Secure` cookie flag; no protection against repeated login attempts; no limit on the size of a JSON body.
- The dialogs do not trap the keyboard focus.
- The base images use floating tags; no dependency audit or image scan is part of the pipeline; Semgrep is a documented manual step.
- One browser (Chromium) was tested end to end; no accessibility, load or concurrency tests.
- The repository is **private**: the trainer needs to be given access (pending the trainer's GitHub username).
- The first build with Docker downloads several hundred megabytes and takes several minutes.

## 6. Final validation (Exercise 6)

Done on 2026-11-16 from a fresh clone of the repository at the commit that precedes the tag (`9ab45f6`), following only the README. The scratch copy and its containers were deleted afterwards.

| Task of the exercise | Result |
|---|---|
| **1. Run the application from the README** | `cp .env.example .env`, then `docker compose up -d --build --wait`. The images were built and all three services were healthy **65 seconds** after the command (the backend itself started in 9.6 s). Flyway migrated an empty schema to version 1000. |
| **2. Confirm the state of the containers** | `docker compose ps`: `db`, `backend` and `web` all `healthy`; only `web` is published, on `127.0.0.1`. The containers run as the non-root users `matriz` and `nginx`. |
| **3. Test the essential functions** | The pipeline's smoke test (the page, a client-side route, the security headers, the backend health, a `401` without a session, a login as a demo user, the profile, the project list) passed. The 4 end-to-end tests passed against this stack. After `docker compose down` and `up`, a project created before was still there (data persists); it was deleted afterwards. |
| **4. Confirm the results of the pipeline** | Green for the same code: [run 43967409485](https://github.com/roSlate/matriz/actions/runs/43967409485), and the whole history in section 2. |
| **5. Consult the logs** | One line matched "warn" in all services: PostgreSQL's standard initialisation notice about local authentication in its official image. No error or exception. |
| **6. Check for credentials in the versioned files** | A scan of the tracked files for private keys, cloud keys, GitHub and API tokens and JWTs found none. The passwords that appear are placeholders (`change-me-locally`), a throwaway used only in CI, and the documented fictional demo password. No `.env` or key file is tracked, and `.env` was never in the Git history. No credential is in the images either. |
| **7. Register the problems and the fixes** | Section 3. |
| **Final review by the AI agent** | The review of the whole implementation ([verification.md](verification.md), section 4), the quality and security review ([quality-security-review.md](quality-security-review.md)), and this validation. |

**Deliverables of the ficha**

| Deliverable | Where |
|---|---|
| Updated source code | The repository, tag `v1.0.0-rc.1` |
| Dockerfiles and `.dockerignore` | `backend/` and `frontend/` |
| Docker Compose configuration | `compose.yaml` |
| `.env.example` without real credentials | `.env.example` |
| GitHub Actions workflow | `.github/workflows/ci.yml` |
| README and technical documentation | `README.md`, `docs/` (the architecture diagrams are in Mermaid in [architecture.md](architecture.md)) |
| Changelog and release notes | `CHANGELOG.md`, `docs/release-notes-1.0.0-rc.1.md` |
| Evidence of the tests and verifications | [verification.md](verification.md), [test-plan.md](test-plan.md), [quality-security-review.md](quality-security-review.md), the Actions history |
| Short report | This document |
| Not included | Passwords, tokens, `.env` files with real values |

## 7. Optional challenges

| Challenge | State |
|---|---|
| A, security: a dependency or image scanner in the pipeline | **Not done.** Semgrep was run by hand (code analysis, not a dependency scan). A dependency and image scanner is the logical next step. |
| B, observability: structured logs and extra availability checks | **Not done**; the services have healthchecks and the application reports its health. |
| C, releases: publishing an image to the GitHub Container Registry | **Not done**; nothing is published (a release candidate, with a tag only). |

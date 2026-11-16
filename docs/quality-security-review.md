# Matriz: Quality and security review

Status: Ficha 04, Exercise 5. Related documents: [test-plan.md](test-plan.md), [verification.md](verification.md) (section 4 holds the earlier review of the whole implementation), [constraints.md](constraints.md), [architecture.md](architecture.md).

This review was done by an AI coding agent working under the rules of [../AGENTS.md](../AGENTS.md) and approved item by item by the developer. The agent was asked to separate **facts observed** from **hypotheses**, to change nothing without approval, and to propose one regression test per fix. Date: 2026-11-16.

## 1. Scope and method

| Part | What was reviewed | How |
|---|---|---|
| **Code and configuration review** | The most recent changes: the Docker images, the nginx configuration, `compose.yaml`, the CI workflow and the end-to-end tests. The earlier review of the whole implementation (permissions, validation, errors, scope) is in [verification.md](verification.md), section 4. | Reading the files, then probing the running stack with `curl` and a browser (read-only). |
| **Authorization between two accounts** | Whether one user can read or change another user's resources, including by changing identifiers. | Two demo accounts, by API and through the interface (section 3). |
| **Static analysis** | All files tracked by Git: Java, TypeScript, React, Dockerfiles, GitHub Actions, nginx, SQL, Python. | Semgrep (section 4). |

## 2. Findings and decisions

Each finding says what was observed, how it was confirmed, and what was decided. "Confirmed" means observed on the running application; "suspected" means a possible problem that could not be confirmed; "false positive" means the tool or the reading was wrong.

| # | Finding | Evidence | Severity | Status | Decision |
|---|---|---|---|---|---|
| **F1** | The pages served by nginx had **no security headers**: no `X-Frame-Options`, `X-Content-Type-Options`, `Referrer-Policy` or Content-Security-Policy. The login page could be embedded in another site (clickjacking). API answers were fine: Spring sets its own headers. | `curl -I` on `/` showed only `Server` and `Cache-Control`. | Medium | **Confirmed** | **Fixed**: headers in `frontend/security-headers.conf`, included in the three locations that nginx serves itself and not in `/api/` (a second copy of a header such as `X-Frame-Options` would be invalid). The policy is strict: same origin only, no inline scripts or styles, `frame-ancestors 'none'`. |
| **F2** | The `Server: nginx/1.30.6` header revealed the exact version. | Same probe. | Low | **Confirmed** | **Fixed**: `server_tokens off`. |
| **F3** | The base images use floating tags (`21-jre-alpine`, `stable-alpine`, `node:24-alpine`), so a rebuild months from now can differ. | The Dockerfiles. | Low | Confirmed | **Accepted** and recorded as a limitation. Pinning digests is possible but needs upkeep; the GitHub Actions are already pinned to commits. |
| **F4** | In the whole-application mode the database port stays published on `127.0.0.1:5432`. | `docker ps`: open from the host; the backend port 8080 is closed. | Info | Confirmed, intentional | **Accepted**: it keeps the development workflow (`docker compose up -d db`) working, and it is bound to loopback only. |
| **F5** | No HTTPS and no `Secure` flag on the session cookie. | Known; [constraints.md](constraints.md). | Info | Confirmed | **Accepted**: HTTPS belongs to a real deployment. |
| **S1** | Semgrep: "bcrypt hash detected", three times, in `db/demo/V1000__demo_data.sql`. | The three demo users share the BCrypt hash of the password `matriz-demo`. | Info | **False positive** in intent | **Accepted**: the password is fictional, documented in the README, and the file is loaded only by the `dev` profile. The migration was **not** edited to silence the tool: changing an applied Flyway migration, even a comment, changes its checksum and would break existing databases. |
| **S2** | Semgrep: "hardcoded password as a default argument" in `docs/verification/verify-api.py`. | `login(email, password="matriz-demo")`. | Info | **False positive** | **Accepted**: the demo password of fictional accounts, in a development-only script. |
| **S3** | Semgrep: `proxy_set_header Host $host;` in `frontend/nginx.conf` passes an attacker-controlled `Host` header to the backend. | See below. | Low | **Suspected, not confirmed** | **No change.** It matters only when the application builds absolute URLs from the `Host` (reset links, redirects). Nothing does: no code reads the host, scheme or port of the request, no `Location` header or absolute URL is produced, Spring ignores `X-Forwarded-*`, and a forged `Host: evil.example` is not echoed. To revisit if such a feature is ever added. |

**S3, how it was checked.** A search of the backend for `getServerName`, `getRequestURL`, `Host`, `X-Forwarded`, `UriComponentsBuilder`, `sendRedirect` and `Location` found nothing. A request through nginx with `Host: evil.example` answered a normal `401` and the text "evil.example" appeared nowhere in the answer.

**Checked and found correct** (no finding):
- The backend port is not reachable from the host; the containers run as non-root users (`matriz`, `nginx`); no credential is in either image (checked in the image environment); the logs of the stack show no errors.
- The workflow has `permissions: contents: read`, uses no secret (the only password is a throwaway created inside the run), does not use `pull_request_target`, and pins every action to a commit.
- The `.env` file was never committed (checked in the history) and is ignored by Git.

## 3. Authorization review with two accounts (IDOR and broken access control)

**Question:** can a user consult or change a resource of a project they do not belong to, even by changing identifiers?

**Setup:** the demo accounts Ana and Sofia. "Team handbook" (project 3) belongs to Sofia; Ana has no access to it.

| Test | Result |
|---|---|
| Ana tries 15 different operations on Sofia's project and on a task of it, by identifier: read the project, list its tasks and members, edit, delete, set its manual progress, add a member, create a task, read, edit and delete its task, read and write its comments, assign a user, and edit its task with an **invalid** body | `404` for every one, also with the invalid body. Sofia's data was unchanged ([verification.md](verification.md), case 08). |
| A project or task that does not exist | The same `404`, so existence is not revealed. |
| A Member and a Viewer try Owner-only operations (edit or delete the project, manage members, set the manual progress, remove the Owner) | `403`. Even the Owner cannot be removed (`409`). |
| The same from the interface: Ana opens `/projects/3` | The page says "This project no longer exists, or you no longer have access to it" and shows nothing about the project (end-to-end test). |
| A request that tries to supply the owner or the author, or any unknown field | `400`, nothing saved. |

**Result: no broken access control was found.** The guard is the single access step in front of every route that has an identifier ([architecture.md](architecture.md), section 5), and nine deliberate removals of a guard, each making tests fail, show that the tests protect it ([verification.md](verification.md), section 1).

## 4. Static analysis

| Item | Value |
|---|---|
| Tool | Semgrep 1.184.0, the official image `semgrep/semgrep` (digest `sha256:cc1b6826…22695ed`) |
| Rule sets | `p/default`, `p/java`, `p/typescript`, `p/react`, `p/dockerfile`, `p/github-actions`, `p/owasp-top-ten` (677 rules) |
| Scanned | 164 files tracked by Git: 68 Java, 50 TypeScript, 5 YAML, 5 JSON, 2 Dockerfiles, 1 Python, 1 HTML, and the SQL, nginx and other files covered by the generic rules; dependencies and build output excluded |
| Privacy | The repository was mounted read-only and the scan ran locally with metrics off; only the rule sets were downloaded. No code left the machine. |
| Result | **5 findings**: S1 (three matches), S2, S3. **None in the Java, TypeScript, React, Dockerfile or GitHub Actions rules.** All five were triaged above: four are accepted false positives and one is a suspicion that could not be confirmed. No confirmed vulnerability. |

To repeat it from the repository root:

```bash
docker run --rm -v "$PWD:/src:ro" semgrep/semgrep semgrep scan --metrics=off \
  --config p/default --config p/java --config p/typescript --config p/react \
  --config p/dockerfile --config p/github-actions --config p/owasp-top-ten \
  --exclude node_modules --exclude target --exclude dist /src
```

**Not added to the pipeline.** Three of the five findings are accepted false positives, so a blocking job would need ignore rules for them, and the image is large (1.6 GB). The scan is a documented manual step; adding it to the pipeline with an ignore file is the next step if wanted.

## 5. Recommendations of the AI, and what was decided

| Recommendation | Decision | Note |
|---|---|---|
| Fix F1 and F2 in `nginx.conf` | **Accepted**, after the developer's review. | See the regression tests below. |
| Allow inline style attributes in the policy (`style-src-attr 'unsafe-inline'`), because the progress bar sets its width with a style attribute. | **Corrected by testing, then dropped.** A control run showed the exception is unnecessary: React sets the width through the DOM (`element.style`), which a policy does not block. The final policy is stricter than the one first proposed. | A proposal of the agent that its own check disproved. |
| Silence the three bcrypt matches by editing the demo migration | **Rejected.** It would change the migration's checksum and break existing databases. | |
| Change `proxy_set_header Host $host` | **Not applied**: no confirmed risk (S3). | |
| Add Semgrep to the pipeline now | **Deferred** (section 4). | |

## 6. Regression tests for the fixes

| Fix | Test | Proven to fail without the fix? |
|---|---|---|
| F1, F2: headers and no version | The "Smoke test through the web service" step of `.github/workflows/ci.yml` asserts the four headers and the Content-Security-Policy on the home page, on a client-side route and on a built file, no nginx version in `Server`, and exactly one copy of the headers on API answers. | **Yes:** with the old nginx configuration the step fails on `X-Frame-Options`. |
| F1: the policy must not break the interface | End-to-end test "the pages load under the Content-Security-Policy, without violations, and the progress bar is drawn". | **Yes:** with a policy that blocks scripts the test fails (the application never renders). |

Both run in the pipeline on every push; the run that contains them succeeded: [run 43964025147](https://github.com/roSlate/matriz/actions/runs/43964025147).

## 7. Checks executed and checks not executed

**Executed:** the code and configuration review of the recent changes; the review of the whole implementation ([verification.md](verification.md), section 4); the two-account authorization review; the Semgrep scan; the smoke test, the end-to-end tests and all other tests, locally and in the pipeline.

**Not executed:**
- A dependency vulnerability audit (for example `npm audit` or a Maven dependency checker) and a scan of the container images: they need network access to vulnerability databases and were not part of this review.
- A scan of the Git history for secrets with a dedicated tool (the history was checked by hand for `.env`).
- A dynamic security test of the running application with an attack tool, a penetration test, and any test over HTTPS.
- A rate-limiting test: the application has no protection against repeated login attempts (accepted, [constraints.md](constraints.md)).

## 8. Pending issues

- Accepted limitations: F3, F4 and F5 above, and those in [verification.md](verification.md), section 7.
- To revisit if the application ever builds absolute URLs from the request: S3.

#!/usr/bin/env python3
"""Scripted check of the nine cases of Ficha 03, exercise 8, through the Vite proxy (:5173).

Usage (development only: needs the backend started with the dev profile, the Vite dev server,
the demo accounts and the database container from compose.yaml):

    python3 docs/verification/verify-api.py 1      # cases 01, 02, 04 to 08; creates throwaway data
    (restart the backend)
    python3 docs/verification/verify-api.py 2      # case 03 (persistence), case 09; cleans up

Phase 2 ends by checking that the database row counts are back to what phase 1 found.
Every check prints: case, what is checked, expected, got, OK or FAIL. The exit code is 1 on any FAIL."""
import json, os, subprocess, sys, tempfile, http.cookiejar, urllib.request, urllib.error

BASE = "http://localhost:5173/api/v1"
STATE = os.path.join(tempfile.gettempdir(), "matriz-verify-state.json")
failures = 0


class Client:
    def __init__(self):
        self.jar = http.cookiejar.CookieJar()
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.jar))

    def token(self):
        for c in self.jar:
            if c.name == "XSRF-TOKEN":
                return c.value
        return None

    def call(self, method, path, body=None, raw=None, csrf=True):
        if self.token() is None:
            self._raw("GET", "/users/me")
        return self._raw(method, path, body, raw, csrf)

    def _raw(self, method, path, body=None, raw=None, csrf=True):
        data = None
        headers = {"Accept": "application/json"}
        if body is not None or raw is not None:
            data = raw.encode() if raw is not None else json.dumps(body).encode()
            headers["Content-Type"] = "application/json"
        if method != "GET" and csrf and self.token():
            headers["X-XSRF-TOKEN"] = self.token()
        req = urllib.request.Request(BASE + path, data=data, method=method, headers=headers)
        try:
            with self.opener.open(req) as r:
                text = r.read().decode()
                return r.status, (json.loads(text) if text else None)
        except urllib.error.HTTPError as e:
            text = e.read().decode()
            try:
                return e.code, json.loads(text)
            except Exception:
                return e.code, text


def login(email, password="matriz-demo"):
    c = Client()
    status, _ = c.call("POST", "/auth/login", {"email": email, "password": password})
    assert status == 200, f"login failed for {email}: {status}"
    return c


def check(case, what, expected, got, note=""):
    global failures
    ok = (got in expected) if isinstance(expected, set) else (got == expected)   # a set means 'any of these'; lists and tuples are compared for equality
    if not ok:
        failures += 1
    print(f"case {case} | {what:<78} | expected {str(expected):<10} got {str(got):<10} {'OK' if ok else 'FAIL'} {note}")


def sql(query):
    out = subprocess.check_output(
        ["docker", "exec", "matriz-db-1", "sh", "-c",
         f'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -tAc "{query}"'], text=True).strip()
    return out


COUNTS = ("select (select count(*) from users),(select count(*) from projects),(select count(*) from project_members),"
          "(select count(*) from tasks),(select count(*) from task_assignments),(select count(*) from comments)")


def phase1():
    state = {"counts_before": sql(COUNTS)}
    print("database counts before (users|projects|memberships|tasks|assignments|comments):", state["counts_before"])
    ana, rui, sofia = login("ana@example.org"), login("rui@example.org"), login("sofia@example.org")

    # ---- case 01: access to the application ----
    anon = Client()
    s, _ = anon.call("GET", "/users/me")
    check("01", "request without a session is refused", 401, s)
    s, _ = anon.call("GET", "/projects")
    check("01", "project list without a session is refused", 401, s)
    s, b1 = Client().call("POST", "/auth/login", {"email": "ana@example.org", "password": "wrong-password"})
    s2, b2 = Client().call("POST", "/auth/login", {"email": "nobody@example.org", "password": "wrong-password"})
    check("01", "wrong password is refused", 401, s)
    check("01", "unknown email is refused", 401, s2)
    check("01", "both refusals carry the same message", True, b1.get("detail") == b2.get("detail"), repr(b1.get("detail")))
    s, me = ana.call("GET", "/users/me")
    check("01", "valid login: /users/me answers 200", 200, s)
    check("01", "the profile has no password or hash", True, "password" not in json.dumps(me).lower())
    nocsrf = Client()
    nocsrf._raw("GET", "/users/me")
    s, _ = nocsrf._raw("POST", "/auth/login", {"email": "ana@example.org", "password": "matriz-demo"}, csrf=False)
    check("01", "login without the CSRF token is refused", 403, s)
    tmp = login("ana@example.org")
    s, _ = tmp.call("POST", "/auth/logout")
    check("01", "logout", 204, s)
    s, _ = tmp.call("GET", "/users/me")
    check("01", "the old session no longer works", 401, s)

    # ---- case 02: project management according to permissions ----
    s, p = ana.call("POST", "/projects", {"name": "  QA verification project  ", "description": "Throwaway."})
    check("02", "create a project", 201, s)
    pid = p["id"]
    check("02", "the creator is the Owner and the name is trimmed", ("OWNER", "QA verification project"), (p["myRole"], p["name"]))
    s, _ = ana.call("POST", "/projects", {"name": "Bad", "ownerId": 2})
    check("02", "owner supplied by the client is refused", 400, s)
    s, lst = ana.call("GET", "/projects?search=verification")
    check("02", "search finds it by part of the name", pid, lst["items"][0]["id"] if lst["items"] else None)
    s, lst = sofia.call("GET", "/projects?search=verification")
    check("02", "a user without access does not see it in the search", 0, lst["totalItems"])
    s, _ = ana.call("POST", f"/projects/{pid}/members", {"email": "rui@example.org", "role": "MEMBER"})
    check("02", "add Rui as Member", 201, s)
    s, _ = ana.call("POST", f"/projects/{pid}/members", {"email": "sofia@example.org", "role": "VIEWER"})
    check("02", "add Sofia as Viewer", 201, s)
    s, e = ana.call("PATCH", f"/projects/{pid}", {"name": "QA verification project (edited)"})
    check("02", "the Owner edits the project", 200, s)
    s, _ = rui.call("PATCH", f"/projects/{pid}", {"name": "Hacked"})
    check("02", "a Member cannot edit the project", 403, s)
    s, _ = sofia.call("PATCH", f"/projects/{pid}", {"name": "Hacked"})
    check("02", "a Viewer cannot edit the project", 403, s)
    s, _ = rui.call("DELETE", f"/projects/{pid}")
    check("02", "a Member cannot delete the project", 403, s)
    state["pid"] = pid

    # ---- case 04: data validation ----
    before = sql("select count(*) from projects")
    for label, body in [("empty name", {"name": ""}), ("spaces-only name", {"name": "     "}), ("2-character name", {"name": "ab"}),
                        ("101-character name", {"name": "x" * 101}), ("1001-character description", {"name": "Valid name", "description": "x" * 1001}),
                        ("number as name", {"name": 42})]:
        s, _ = ana.call("POST", "/projects", body)
        check("04", f"project: {label}", 400, s)
    check("04", "no project was created by the invalid requests", before, sql("select count(*) from projects"))
    for label, body in [("empty title", {"title": ""}), ("spaces-only title", {"title": "   "}), ("2-character title", {"title": "ab"}),
                        ("151-character title", {"title": "x" * 151}), ("2001-character description", {"title": "Valid", "description": "x" * 2001}),
                        ("invalid status", {"title": "Valid", "status": "DONE!"}), ("invalid priority", {"title": "Valid", "priority": "URGENT"})]:
        s, _ = ana.call("POST", f"/projects/{pid}/tasks", body)
        check("04", f"task: {label}", 400, s)
    check("04", "no task was created by the invalid requests", "0", sql(f"select count(*) from tasks where project_id={pid}"))

    # ---- case 03 (first half) and 05: tasks, status and priority ----
    s, t1 = ana.call("POST", f"/projects/{pid}/tasks", {"title": "  First verification task  "})
    check("03", "create a task", 201, s)
    check("03", "defaults are TODO and MEDIUM, the title is trimmed", ("TODO", "MEDIUM", "First verification task"),
          (t1["status"], t1["priority"], t1["title"]))
    s, lst = rui.call("GET", f"/projects/{pid}/tasks")
    check("03", "the task is in the list straight away, exactly once", [t1["id"]], [i["id"] for i in lst["items"]])
    s, t2 = rui.call("POST", f"/projects/{pid}/tasks", {"title": "Second verification task", "priority": "HIGH"})
    s, t3 = ana.call("POST", f"/projects/{pid}/tasks", {"title": "Third verification task", "status": "IN_PROGRESS"})
    s, pr = ana.call("GET", f"/projects/{pid}")
    check("05", "counts before the edit (todo, in progress, done)", (2, 1, 0), (pr["taskCounts"]["todo"], pr["taskCounts"]["inProgress"], pr["taskCounts"]["done"]))
    s, t = rui.call("PATCH", f"/tasks/{t1['id']}", {"status": "DONE"})
    check("05", "a Member sets the status to DONE", 200, s)
    s, pr = ana.call("GET", f"/projects/{pid}")
    check("05", "counts follow the edit", (1, 1, 1, 3), (pr["taskCounts"]["todo"], pr["taskCounts"]["inProgress"], pr["taskCounts"]["done"], pr["taskCounts"]["total"]))
    check("05", "computed progress is 1 of 3 = 33 %", 33, pr["progress"]["computed"])
    s, lst = ana.call("GET", f"/projects/{pid}/tasks?status=DONE")
    check("05", "the DONE filter shows exactly the edited task", [t1["id"]], [i["id"] for i in lst["items"]])
    s, lst = ana.call("GET", f"/projects/{pid}/tasks?status=TODO&priority=HIGH")
    check("05", "status and priority filters combine", [t2["id"]], [i["id"] for i in lst["items"]])
    s, _ = rui.call("PATCH", f"/tasks/{t1['id']}", {"status": "TODO"})
    check("05", "a DONE task can be reopened", 200, s)
    s, _ = sofia.call("PATCH", f"/tasks/{t1['id']}", {"status": "DONE"})
    check("05", "a Viewer cannot change a task", 403, s)

    # ---- case 06: assignees ----
    s, a = ana.call("POST", f"/tasks/{t1['id']}/assignees", {"userId": 2})
    check("06", "assign Rui", 201, s)
    s, a = rui.call("POST", f"/tasks/{t1['id']}/assignees", {"userId": 1})
    check("06", "assign Ana (several assignees)", 201, s)
    check("06", "both are assignees", ["Ana Silva", "Rui Costa"], [x["name"] for x in a["assignees"]])
    s, _ = ana.call("POST", f"/tasks/{t1['id']}/assignees", {"userId": 2})
    check("06", "the same assignment twice is refused", 409, s)
    check("06", "no duplicate row", "2", sql(f"select count(*) from task_assignments where task_id={t1['id']}"))
    s, _ = ana.call("POST", f"/tasks/{t1['id']}/assignees", {"userId": 3})
    check("06", "a Viewer cannot be assigned", 422, s)
    s, _ = sofia.call("POST", f"/tasks/{t1['id']}/assignees", {"userId": 1})
    check("06", "a Viewer cannot assign", 403, s)
    s, _ = ana.call("DELETE", f"/tasks/{t1['id']}/assignees/2")
    check("06", "unassign Rui", 204, s)
    s, tk = ana.call("GET", f"/tasks/{t1['id']}")
    check("06", "only Ana remains", ["Ana Silva"], [x["name"] for x in tk["assignees"]])

    # ---- case 07: comments ----
    s, c1 = rui.call("POST", f"/tasks/{t1['id']}/comments", {"content": "  Looks good to me.  "})
    check("07", "a Member comments", 201, s)
    check("07", "author is the session user, text trimmed, date set by the server", ("Rui Costa", "Looks good to me.", True),
          (c1["author"]["name"], c1["content"], bool(c1.get("createdAt"))))
    s, _ = rui.call("POST", f"/tasks/{t1['id']}/comments", {"content": "x", "authorId": 1})
    check("07", "a client-supplied author is refused", 400, s)
    s, _ = rui.call("POST", f"/tasks/{t1['id']}/comments", {"content": "    "})
    check("07", "a spaces-only comment is refused", 400, s)
    s, _ = rui.call("POST", f"/tasks/{t1['id']}/comments", {"content": "x" * 2001})
    check("07", "a 2001-character comment is refused", 400, s)
    s, _ = sofia.call("POST", f"/tasks/{t1['id']}/comments", {"content": "Viewer comment"})
    check("07", "a Viewer cannot comment", 403, s)
    s, lst = sofia.call("GET", f"/tasks/{t1['id']}/comments")
    check("07", "a Viewer can read the comment, with the author's name and no email", (1, True),
          (lst["totalItems"], "email" not in json.dumps(lst)))

    # ---- case 08: authorization in the API, changing identifiers ----
    s, ht = sofia.call("POST", "/projects/3/tasks", {"title": "Private handbook task"})
    check("08", "(setup) Sofia creates a task in her private project", 201, s)
    hid = ht["id"]
    state["handbook_task"] = hid
    s, _ = sofia.call("POST", f"/tasks/{hid}/comments", {"content": "Private comment"})
    for label, method, path, body in [
        ("read the other project", "GET", "/projects/3", None),
        ("list its tasks", "GET", "/projects/3/tasks", None),
        ("list its members", "GET", "/projects/3/members", None),
        ("edit it", "PATCH", "/projects/3", {"name": "Hijacked"}),
        ("delete it", "DELETE", "/projects/3", None),
        ("set its manual progress", "PUT", "/projects/3/progress-override", {"percentage": 10}),
        ("add a member to it", "POST", "/projects/3/members", {"email": "tom@example.org", "role": "MEMBER"}),
        ("create a task in it", "POST", "/projects/3/tasks", {"title": "Intruder task"}),
        ("read its task by identifier", "GET", f"/tasks/{hid}", None),
        ("edit its task", "PATCH", f"/tasks/{hid}", {"status": "DONE"}),
        ("delete its task", "DELETE", f"/tasks/{hid}", None),
        ("read its comments", "GET", f"/tasks/{hid}/comments", None),
        ("comment on its task", "POST", f"/tasks/{hid}/comments", {"content": "Intruder"}),
        ("assign on its task", "POST", f"/tasks/{hid}/assignees", {"userId": 1}),
        ("edit its task with an INVALID body (must still be 404)", "PATCH", f"/tasks/{hid}", {"title": ""}),
    ]:
        s, _ = ana.call(method, path, body)
        check("08", f"Ana (no access) tries to {label}", 404, s)
    check("08", "Sofia's private data is unchanged", ("Team handbook", "Private handbook task"),
          (sql("select name from projects where id=3"), sql(f"select title from tasks where id={hid}")))
    s, _ = ana.call("GET", "/projects/999999")
    check("08", "a project that does not exist looks the same (404)", 404, s)
    s, _ = rui.call("DELETE", f"/projects/{pid}/members/1")
    check("08", "a Member cannot remove the Owner", 403, s)
    s, _ = ana.call("DELETE", f"/projects/{pid}/members/1")
    check("08", "even the Owner cannot be removed", 409, s)

    json.dump(state, open(STATE, "w"))
    print("phase 1 done; failures so far:", failures)


def phase2():
    state = json.load(open(STATE))
    pid, hid = state["pid"], state["handbook_task"]
    ana, rui, sofia = login("ana@example.org"), login("rui@example.org"), login("sofia@example.org")

    # ---- case 03 (second half): persistence after the backend restart ----
    s, lst = rui.call("GET", f"/projects/{pid}/tasks")
    check("03", "after the backend restart all 3 tasks are still there", 3, lst["totalItems"])
    s, lst2 = ana.call("GET", "/projects?search=verification")
    check("03", "the project is still there after the restart", pid, lst2["items"][0]["id"])
    s, lst3 = sofia.call("GET", f"/tasks/{lst['items'][-1]['id']}/comments")
    check("03", "comments are still there after the restart", 1, lst3["totalItems"])

    # ---- case 09: deletion respects dependencies; errors give no success and no data loss ----
    s, pr = ana.call("GET", f"/projects/{pid}")
    check("09", "the project has 3 tasks before deletion", 3, pr["taskCounts"]["total"])
    oldest = lst["items"][-1]["id"]   # the first task: it has an assignment and a comment
    check("09", "that task has an assignment and a comment before deletion", ("1", "1"),
          (sql(f"select count(*) from task_assignments where task_id={oldest}"), sql(f"select count(*) from comments where task_id={oldest}")))
    s, _ = ana.call("DELETE", f"/tasks/{oldest}")
    check("09", "delete that task", 204, s)
    check("09", "its assignments and comments went with it, the other tasks stay", ("0", "0", 2),
          (sql(f"select count(*) from task_assignments where task_id={oldest}"), sql(f"select count(*) from comments where task_id={oldest}"),
           ana.call("GET", f"/projects/{pid}")[1]["taskCounts"]["total"]))
    s, _ = sofia.call("DELETE", f"/tasks/{hid}")
    check("09", "(cleanup) Sofia deletes her temporary task", 204, s)
    s, _ = ana.call("DELETE", f"/projects/{pid}")
    check("09", "the Owner deletes the project", 204, s)
    check("09", "no tasks, memberships, assignments or comments of it remain", "0|0",
          sql(f"select (select count(*) from tasks where project_id={pid}) || '|' || (select count(*) from project_members where project_id={pid})"))
    s, _ = ana.call("GET", f"/projects/{pid}")
    check("09", "the deleted project answers 404, never a success", 404, s)
    s, _ = ana.call("DELETE", f"/projects/{pid}")
    check("09", "deleting it again answers 404", 404, s)
    s, b = ana.call("POST", "/projects", raw="not json")
    check("09", "a malformed body is refused with 400", 400, s)
    check("09", "the error body has no internals (no stack trace, class or SQL)", True,
          not any(w in json.dumps(b) for w in ("Exception", "java.", "org.springframework", "SELECT", "at com.")))
    s, b = ana.call("GET", "/nothing-here")
    check("09", "an unknown route gives the fixed message", "Resource not found.", b.get("detail") if isinstance(b, dict) else b)
    after = sql(COUNTS)
    check("09", "database counts are back to the original", state["counts_before"], after)
    print("phase 2 done; failures in total:", failures)


if __name__ == "__main__":
    {"1": phase1, "2": phase2}[sys.argv[1]]()
    sys.exit(1 if failures else 0)

import { useEffect, useRef, useState } from "react";
import { errorMessage } from "../../api/client";
import { listMembers, type Member } from "../../api/members";
import { assignUser, unassignUser, type Assignee, type Task } from "../../api/tasks";
import { Alert } from "../../components/ui/Alert";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { SelectField } from "../../components/ui/SelectField";

type MembersState = { status: "loading" } | { status: "error" } | { status: "ready"; members: Member[] };

type AssigneesPanelProps = {
  task: Task;
  /** Owners and Members may change the assignees; Viewers only read them. */
  canWrite: boolean;
  /** Called with the assignees the server confirmed, so the page shows them. */
  onChanged: (assignees: Assignee[]) => void;
};

/**
 * The people assigned to a task. Anyone who can see the task sees them ("Unassigned" when there
 * are none). Owners and Members can also assign and unassign. The select offers only the project's
 * Owners and Members who are not yet assigned; the server checks eligibility again.
 */
export function AssigneesPanel({ task, canWrite, onChanged }: AssigneesPanelProps) {
  const [membersState, setMembersState] = useState<MembersState>({ status: "loading" });
  const [reloadKey, setReloadKey] = useState(0);
  const [selected, setSelected] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const inFlight = useRef(false); // blocks a second request before the first has re-rendered

  useEffect(() => {
    if (!canWrite) {
      return;
    }
    let cancelled = false;
    setMembersState({ status: "loading" });
    listMembers(task.projectId)
      .then((members) => !cancelled && setMembersState({ status: "ready", members }))
      .catch(() => !cancelled && setMembersState({ status: "error" }));
    return () => {
      cancelled = true;
    };
  }, [canWrite, task.projectId, reloadKey]);

  async function run(action: () => Promise<void>) {
    if (inFlight.current) {
      return;
    }
    inFlight.current = true;
    setBusy(true);
    setError(null);
    try {
      await action();
    } catch (failure) {
      setError(errorMessage(failure));
    } finally {
      inFlight.current = false;
      setBusy(false);
    }
  }

  const assign = () =>
    run(async () => {
      const updated = await assignUser(task.id, Number(selected));
      setSelected("");
      onChanged(updated.assignees);
    });

  const unassign = (person: Assignee) =>
    run(async () => {
      await unassignUser(task.id, person.id);
      onChanged(task.assignees.filter((item) => item.id !== person.id));
    });

  const assignedIds = new Set(task.assignees.map((person) => person.id));
  const eligible =
    membersState.status === "ready"
      ? membersState.members.filter((member) => member.role !== "VIEWER" && !assignedIds.has(member.userId))
      : [];

  return (
    <section aria-labelledby="assignees-heading">
      <h2 id="assignees-heading" className="section-title">
        Assignees
      </h2>

      {error && <Alert tone="error">{error}</Alert>}

      {task.assignees.length === 0 ? (
        <p className="muted">Unassigned</p>
      ) : (
        <ul className="chips" aria-label="Assignees">
          {task.assignees.map((person) => (
            <li key={person.id} className="chip">
              <Badge>{person.name}</Badge>
              {canWrite && (
                <button
                  type="button"
                  className="chip__remove"
                  aria-label={`Unassign ${person.name}`}
                  disabled={busy}
                  onClick={() => void unassign(person)}
                >
                  ×
                </button>
              )}
            </li>
          ))}
        </ul>
      )}

      {canWrite && membersState.status === "loading" && <p role="status">Loading people…</p>}

      {canWrite && membersState.status === "error" && (
        <Alert tone="error">
          <p className="alert__text">Could not load the people to assign.</p>
          <Button variant="secondary" onClick={() => setReloadKey((key) => key + 1)}>
            Try again
          </Button>
        </Alert>
      )}

      {canWrite && membersState.status === "ready" &&
        (eligible.length === 0 ? (
          <p className="muted">Everyone who can be assigned is already assigned.</p>
        ) : (
          <div className="form-row">
            <SelectField
              label="Assign to"
              options={[
                { value: "", label: "Select a person" },
                ...eligible.map((member) => ({ value: String(member.userId), label: member.name })),
              ]}
              value={selected}
              disabled={busy}
              onChange={(event) => setSelected(event.target.value)}
            />
            <Button disabled={busy || selected === ""} onClick={() => void assign()}>
              Assign
            </Button>
          </div>
        ))}
    </section>
  );
}

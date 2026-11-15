import { useEffect, useState } from "react";
import { errorMessage } from "../../api/client";
import {
  addMember,
  changeMemberRole,
  listMembers,
  removeMember,
  type AssignableRole,
  type Member,
  type MemberInput,
} from "../../api/members";
import type { Project } from "../../api/projects";
import { Alert } from "../../components/ui/Alert";
import { Button } from "../../components/ui/Button";
import { ConfirmDialog } from "../../components/ui/ConfirmDialog";
import { ROLE_LABELS } from "../projects/projectLabels";
import { AddMemberDialog } from "./AddMemberDialog";

type LoadState = { status: "loading" } | { status: "error" } | { status: "ready"; members: Member[] };

/**
 * The members of a project. Every member sees the list; only the Owner sees the controls to add,
 * change and remove. The server enforces the same rule, so hiding the controls is only a courtesy.
 */
export function MembersSection({ project }: { project: Project }) {
  const [state, setState] = useState<LoadState>({ status: "loading" });
  const [reloadKey, setReloadKey] = useState(0);
  const [adding, setAdding] = useState(false);
  const [removing, setRemoving] = useState<Member | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [busyUserId, setBusyUserId] = useState<number | null>(null);
  const isOwner = project.myRole === "OWNER";

  useEffect(() => {
    let cancelled = false;
    setState({ status: "loading" });
    listMembers(project.id)
      .then((members) => !cancelled && setState({ status: "ready", members }))
      .catch(() => !cancelled && setState({ status: "error" }));
    return () => {
      cancelled = true;
    };
  }, [project.id, reloadKey]);

  const reload = () => setReloadKey((key) => key + 1);

  async function handleAdd(input: MemberInput) {
    await addMember(project.id, input);
    setAdding(false);
    reload(); // the list comes back from the server, in its order
  }

  async function handleRoleChange(member: Member, role: AssignableRole) {
    if (busyUserId !== null || role === member.role) {
      return;
    }
    setBusyUserId(member.userId);
    setActionError(null);
    try {
      // The row shows what the server confirmed; on failure the select goes back to the old role.
      const updated = await changeMemberRole(project.id, member.userId, role);
      setState((current) =>
        current.status === "ready"
          ? { status: "ready", members: current.members.map((item) => (item.userId === updated.userId ? updated : item)) }
          : current,
      );
    } catch (error) {
      setActionError(
        errorMessage(error, "Could not change the role. Please try again."),
      );
    } finally {
      setBusyUserId(null);
    }
  }

  async function handleRemove(member: Member) {
    await removeMember(project.id, member.userId);
    setRemoving(null);
    reload();
  }

  return (
    <section className="members" aria-labelledby="members-heading">
      <div className="page-heading">
        <h2 id="members-heading">Members</h2>
        {isOwner && <Button onClick={() => setAdding(true)}>Add member</Button>}
      </div>

      {actionError && <Alert tone="error">{actionError}</Alert>}

      {state.status === "loading" && <p role="status">Loading members…</p>}

      {state.status === "error" && (
        <Alert tone="error">
          <p className="alert__text">Could not load the members.</p>
          <Button variant="secondary" onClick={reload}>
            Try again
          </Button>
        </Alert>
      )}

      {state.status === "ready" && (
        <table className="task-table" aria-label="Project members">
          <thead>
            <tr>
              <th scope="col">Name</th>
              <th scope="col">Email</th>
              <th scope="col">Role</th>
              {isOwner && <th scope="col">Actions</th>}
            </tr>
          </thead>
          <tbody>
            {state.members.map((member) => (
              <tr key={member.userId}>
                <td>{member.name}</td>
                <td>{member.email}</td>
                <td>
                  {isOwner && member.role !== "OWNER" ? (
                    <select
                      aria-label={`Role of ${member.name}`}
                      value={member.role}
                      disabled={busyUserId !== null}
                      onChange={(event) => void handleRoleChange(member, event.target.value as AssignableRole)}
                    >
                      <option value="MEMBER">{ROLE_LABELS.MEMBER}</option>
                      <option value="VIEWER">{ROLE_LABELS.VIEWER}</option>
                    </select>
                  ) : (
                    ROLE_LABELS[member.role]
                  )}
                </td>
                {isOwner && (
                  <td>
                    {member.role !== "OWNER" && (
                      <Button
                        variant="secondary"
                        aria-label={`Remove ${member.name}`}
                        disabled={busyUserId !== null}
                        onClick={() => setRemoving(member)}
                      >
                        Remove
                      </Button>
                    )}
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {adding && <AddMemberDialog onSubmit={handleAdd} onClose={() => setAdding(false)} />}

      {removing && (
        <ConfirmDialog
          title={`Remove ${removing.name}?`}
          message={
            <p>
              {removing.name} will lose access to this project and be unassigned from its tasks. The tasks and
              comments they wrote stay.
            </p>
          }
          confirmLabel="Remove member"
          onConfirm={() => handleRemove(removing)}
          onCancel={() => setRemoving(null)}
        />
      )}
    </section>
  );
}

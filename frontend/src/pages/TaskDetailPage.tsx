import { useEffect, useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ApiError } from "../api/client";
import { getProject, type Project } from "../api/projects";
import {
  deleteTask,
  getTask,
  updateTask,
  type Task,
  type TaskInput,
  type TaskPriority,
  type TaskStatus,
} from "../api/tasks";
import { Alert } from "../components/ui/Alert";
import { Badge } from "../components/ui/Badge";
import { Button } from "../components/ui/Button";
import { Card } from "../components/ui/Card";
import { ConfirmDialog } from "../components/ui/ConfirmDialog";
import { SelectField } from "../components/ui/SelectField";
import { TaskFormDialog } from "../features/tasks/TaskFormDialog";
import { PRIORITY_LABELS, PRIORITY_OPTIONS, STATUS_LABELS, STATUS_OPTIONS } from "../features/tasks/taskLabels";
import "./pages.css";

type LoadState =
  | { status: "loading" }
  | { status: "notFound" }
  | { status: "error" }
  | { status: "ready"; task: Task; project: Project };

const formatDate = (iso: string) =>
  new Date(iso).toLocaleDateString("en-GB", { day: "numeric", month: "short", year: "numeric" });

/** One task: its data, status and priority, assignees, and edit and delete for Owners and Members. */
export default function TaskDetailPage() {
  const { taskId } = useParams();
  const navigate = useNavigate();
  const [state, setState] = useState<LoadState>({ status: "loading" });
  const [reloadKey, setReloadKey] = useState(0);
  const [editing, setEditing] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const inFlight = useRef(false);

  useEffect(() => {
    let cancelled = false;
    setState({ status: "loading" });
    getTask(Number(taskId))
      .then(async (task) => {
        const project = await getProject(task.projectId);
        if (!cancelled) {
          setState({ status: "ready", task, project });
        }
      })
      .catch((error) => {
        if (cancelled) {
          return;
        }
        // 404 covers "does not exist" and "no access" alike; 400 is a malformed identifier.
        const missing = error instanceof ApiError && (error.status === 404 || error.status === 400);
        setState({ status: missing ? "notFound" : "error" });
      });
    return () => {
      cancelled = true;
    };
  }, [taskId, reloadKey]);

  if (state.status === "loading") {
    return <p role="status">Loading task…</p>;
  }
  if (state.status === "notFound") {
    return (
      <Alert tone="error">
        <p className="alert__text">This task no longer exists, or you no longer have access to it.</p>
        <Link to="/">Back to my projects</Link>
      </Alert>
    );
  }
  if (state.status === "error") {
    return (
      <Alert tone="error">
        <p className="alert__text">Could not load this task.</p>
        <Button variant="secondary" onClick={() => setReloadKey((key) => key + 1)}>
          Try again
        </Button>
      </Alert>
    );
  }

  const { task, project } = state;
  const canWrite = project.myRole !== "VIEWER";

  // The screen shows what the server confirmed, never what was only typed or chosen.
  async function save(changes: Partial<TaskInput>) {
    const updated = await updateTask(task.id, changes);
    setState({ status: "ready", task: updated, project });
  }

  async function quickChange(changes: Partial<TaskInput>) {
    if (inFlight.current) {
      return;
    }
    inFlight.current = true;
    setSaving(true);
    setSaveError(null);
    try {
      await save(changes);
    } catch (error) {
      setSaveError(error instanceof ApiError ? error.message : "Something went wrong. Please try again.");
    } finally {
      inFlight.current = false;
      setSaving(false);
    }
  }

  async function handleDelete() {
    await deleteTask(task.id);
    navigate(`/projects/${task.projectId}`, { replace: true });
  }

  return (
    <>
      <nav className="breadcrumb" aria-label="Breadcrumb">
        <Link to="/">My projects</Link>
        <span aria-hidden="true"> › </span>
        <Link to={`/projects/${project.id}`}>{project.name}</Link>
        <span aria-hidden="true"> › </span>
        <span>{task.title}</span>
      </nav>

      <div className="page-heading">
        <h1>{task.title}</h1>
        {canWrite && (
          <div className="page-heading__actions">
            <Button variant="secondary" onClick={() => setEditing(true)}>
              Edit
            </Button>
            <Button variant="secondary" onClick={() => setDeleting(true)}>
              Delete task
            </Button>
          </div>
        )}
      </div>

      <p className={task.description ? undefined : "muted"}>{task.description ?? "No description."}</p>
      <p className="muted">
        Created {formatDate(task.createdAt)} · Updated {formatDate(task.updatedAt)}
      </p>

      {saveError && <Alert tone="error">{saveError}</Alert>}

      <Card>
        {canWrite ? (
          <div className="form-row">
            <SelectField
              label="Status"
              options={STATUS_OPTIONS}
              value={task.status}
              disabled={saving}
              onChange={(event) => void quickChange({ status: event.target.value as TaskStatus })}
            />
            <SelectField
              label="Priority"
              options={PRIORITY_OPTIONS}
              value={task.priority}
              disabled={saving}
              onChange={(event) => void quickChange({ priority: event.target.value as TaskPriority })}
            />
          </div>
        ) : (
          <p>
            <span className="muted">Status:</span> <Badge>{STATUS_LABELS[task.status]}</Badge>{" "}
            <span className="muted">Priority:</span> <Badge>{PRIORITY_LABELS[task.priority]}</Badge>
          </p>
        )}

        <h2 className="section-title">Assignees</h2>
        {task.assignees.length === 0 ? (
          <p className="muted">Unassigned</p>
        ) : (
          <ul className="chips" aria-label="Assignees">
            {task.assignees.map((person) => (
              <li key={person.id}>
                <Badge>{person.name}</Badge>
              </li>
            ))}
          </ul>
        )}
      </Card>

      {editing && (
        <TaskFormDialog
          title="Edit task"
          submitLabel="Save changes"
          initial={{
            title: task.title,
            description: task.description ?? "",
            status: task.status,
            priority: task.priority,
          }}
          onSubmit={async (input) => {
            await save(input);
            setEditing(false);
          }}
          onClose={() => setEditing(false)}
        />
      )}

      {deleting && (
        <ConfirmDialog
          title={`Delete "${task.title}"?`}
          message={<p>Its comments and assignments will be permanently deleted. This cannot be undone.</p>}
          confirmLabel="Delete task"
          onConfirm={handleDelete}
          onCancel={() => setDeleting(false)}
        />
      )}
    </>
  );
}

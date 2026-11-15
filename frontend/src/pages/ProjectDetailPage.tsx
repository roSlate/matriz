import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ApiError } from "../api/client";
import {
  clearProgressOverride,
  deleteProject,
  getProject,
  setProgressOverride,
  updateProject,
  type Project,
  type ProjectInput,
} from "../api/projects";
import { Alert } from "../components/ui/Alert";
import { Badge } from "../components/ui/Badge";
import { Button } from "../components/ui/Button";
import { Card } from "../components/ui/Card";
import { ConfirmDialog } from "../components/ui/ConfirmDialog";
import { ProjectFormDialog } from "../features/projects/ProjectFormDialog";
import { ProgressOverrideDialog } from "../features/projects/ProgressOverrideDialog";
import { ProjectProgress } from "../features/projects/ProjectProgress";
import { ROLE_LABELS } from "../features/projects/projectLabels";
import { MembersSection } from "../features/members/MembersSection";
import { TasksSection } from "../features/tasks/TasksSection";
import "./pages.css";

type LoadState =
  | { status: "loading" }
  | { status: "notFound" }
  | { status: "error" }
  | { status: "ready"; project: Project };

/** One project: its name, role, description, counts and progress, and Edit for the Owner. */
export default function ProjectDetailPage() {
  const { projectId } = useParams();
  const navigate = useNavigate();
  const [state, setState] = useState<LoadState>({ status: "loading" });
  const [editing, setEditing] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [settingProgress, setSettingProgress] = useState(false);
  const [clearingProgress, setClearingProgress] = useState(false);
  const [progressError, setProgressError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let cancelled = false;
    setState({ status: "loading" });
    getProject(Number(projectId))
      .then((project) => !cancelled && setState({ status: "ready", project }))
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
  }, [projectId, reloadKey]);

  // Quietly reloads the project, so the counts and progress follow the tasks without a flash.
  function refreshProject() {
    getProject(Number(projectId))
      .then((project) => setState({ status: "ready", project }))
      .catch(() => undefined);
  }

  async function handleEdit(input: ProjectInput) {
    // The screen shows what the server confirmed, not what was typed.
    const updated = await updateProject(Number(projectId), input);
    setState({ status: "ready", project: updated });
    setEditing(false);
  }

  async function handleSetProgress(percentage: number) {
    // The screen shows what the server confirmed, not what was typed.
    const updated = await setProgressOverride(Number(projectId), percentage);
    setState({ status: "ready", project: updated });
    setSettingProgress(false);
  }

  async function handleClearProgress() {
    if (clearingProgress) {
      return;
    }
    setClearingProgress(true);
    setProgressError(null);
    try {
      const updated = await clearProgressOverride(Number(projectId));
      setState({ status: "ready", project: updated });
    } catch (error) {
      setProgressError(error instanceof ApiError ? error.message : "Could not clear the manual progress. Please try again.");
    } finally {
      setClearingProgress(false);
    }
  }

  async function handleDelete() {
    await deleteProject(Number(projectId));
    navigate("/", { replace: true });
  }

  if (state.status === "loading") {
    return <p role="status">Loading project…</p>;
  }
  if (state.status === "notFound") {
    return (
      <Alert tone="error">
        <p className="alert__text">This project no longer exists, or you no longer have access to it.</p>
        <Link to="/">Back to my projects</Link>
      </Alert>
    );
  }
  if (state.status === "error") {
    return (
      <Alert tone="error">
        <p className="alert__text">Could not load this project.</p>
        <Button variant="secondary" onClick={() => setReloadKey((key) => key + 1)}>
          Try again
        </Button>
      </Alert>
    );
  }

  const { project } = state;
  return (
    <>
      <nav className="breadcrumb" aria-label="Breadcrumb">
        <Link to="/">My projects</Link>
        <span aria-hidden="true"> › </span>
        <span>{project.name}</span>
      </nav>

      <div className="page-heading">
        <div className="page-heading__title">
          <h1>{project.name}</h1>
          <Badge>{ROLE_LABELS[project.myRole]}</Badge>
        </div>
        {project.myRole === "OWNER" && (
          <div className="page-heading__actions">
            <Button variant="secondary" onClick={() => setEditing(true)}>
              Edit
            </Button>
            <Button variant="secondary" onClick={() => setDeleting(true)}>
              Delete project
            </Button>
          </div>
        )}
      </div>

      <p className={project.description ? undefined : "muted"}>{project.description ?? "No description."}</p>

      <Card>
        <ProjectProgress project={project} />
        {project.myRole === "OWNER" && (
          <div className="progress-controls">
            {progressError && <Alert tone="error">{progressError}</Alert>}
            <div className="page-heading__actions">
              <Button variant="secondary" onClick={() => setSettingProgress(true)}>
                {project.progress.manual === null ? "Set manual progress" : "Change manual progress"}
              </Button>
              {project.progress.manual !== null && (
                <Button variant="secondary" disabled={clearingProgress} onClick={() => void handleClearProgress()}>
                  Use computed value
                </Button>
              )}
            </div>
          </div>
        )}
      </Card>

      <TasksSection project={project} onChanged={refreshProject} />

      <MembersSection project={project} />

      {deleting && (
        <ConfirmDialog
          title={`Delete "${project.name}"?`}
          message={
            <p>
              Its {project.taskCounts.total} {project.taskCounts.total === 1 ? "task" : "tasks"}, with their
              comments and assignments, and its members will be permanently deleted. This cannot be undone.
            </p>
          }
          confirmLabel="Delete project"
          onConfirm={handleDelete}
          onCancel={() => setDeleting(false)}
        />
      )}

      {settingProgress && (
        <ProgressOverrideDialog
          initial={project.progress.effective}
          onSubmit={handleSetProgress}
          onClose={() => setSettingProgress(false)}
        />
      )}

      {editing && (
        <ProjectFormDialog
          title="Edit project"
          submitLabel="Save changes"
          initial={{ name: project.name, description: project.description ?? "" }}
          onSubmit={handleEdit}
          onClose={() => setEditing(false)}
        />
      )}
    </>
  );
}

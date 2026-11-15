import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ApiError } from "../api/client";
import { getProject, updateProject, type Project, type ProjectInput } from "../api/projects";
import { Alert } from "../components/ui/Alert";
import { Badge } from "../components/ui/Badge";
import { Button } from "../components/ui/Button";
import { Card } from "../components/ui/Card";
import { ProjectFormDialog } from "../features/projects/ProjectFormDialog";
import { ProjectProgress } from "../features/projects/ProjectProgress";
import { ROLE_LABELS } from "../features/projects/projectLabels";
import "./pages.css";

type LoadState =
  | { status: "loading" }
  | { status: "notFound" }
  | { status: "error" }
  | { status: "ready"; project: Project };

/** One project: its name, role, description, counts and progress, and Edit for the Owner. */
export default function ProjectDetailPage() {
  const { projectId } = useParams();
  const [state, setState] = useState<LoadState>({ status: "loading" });
  const [editing, setEditing] = useState(false);
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

  async function handleEdit(input: ProjectInput) {
    // The screen shows what the server confirmed, not what was typed.
    const updated = await updateProject(Number(projectId), input);
    setState({ status: "ready", project: updated });
    setEditing(false);
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
          <Button variant="secondary" onClick={() => setEditing(true)}>
            Edit
          </Button>
        )}
      </div>

      <p className={project.description ? undefined : "muted"}>{project.description ?? "No description."}</p>

      <Card>
        <ProjectProgress project={project} />
      </Card>

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

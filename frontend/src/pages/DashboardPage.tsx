import { useEffect, useState } from "react";
import {
  createProject,
  listProjects,
  type Page,
  type Project,
  type ProjectInput,
} from "../api/projects";
import { Alert } from "../components/ui/Alert";
import { Button } from "../components/ui/Button";
import { Card } from "../components/ui/Card";
import { ProjectCard } from "../features/projects/ProjectCard";
import { ProjectFormDialog } from "../features/projects/ProjectFormDialog";
import "./pages.css";

type LoadState = { status: "loading" } | { status: "error" } | { status: "ready"; data: Page<Project> };

/** The dashboard: the projects the user can access, as cards with their roles, counts and progress. */
export default function DashboardPage() {
  const [page, setPage] = useState(0);
  const [reloadKey, setReloadKey] = useState(0);
  const [state, setState] = useState<LoadState>({ status: "loading" });
  const [creating, setCreating] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setState({ status: "loading" });
    listProjects(page)
      .then((data) => !cancelled && setState({ status: "ready", data }))
      .catch(() => !cancelled && setState({ status: "error" }));
    return () => {
      cancelled = true;
    };
  }, [page, reloadKey]);

  async function handleCreate(input: ProjectInput) {
    await createProject(input);
    setCreating(false);
    setPage(0); // the new project is the newest, so it is on the first page
    setReloadKey((key) => key + 1);
  }

  return (
    <>
      <div className="page-heading">
        <h1>My projects</h1>
        <Button onClick={() => setCreating(true)}>New project</Button>
      </div>

      {state.status === "loading" && <p role="status">Loading projects…</p>}

      {state.status === "error" && (
        <Alert tone="error">
          <p className="alert__text">Could not load your projects.</p>
          <Button variant="secondary" onClick={() => setReloadKey((key) => key + 1)}>
            Try again
          </Button>
        </Alert>
      )}

      {state.status === "ready" && state.data.totalItems === 0 && (
        <Card>
          <p>You have no projects yet.</p>
          <Button onClick={() => setCreating(true)}>Create your first project</Button>
        </Card>
      )}

      {state.status === "ready" && state.data.totalItems > 0 && (
        <>
          <ul className="project-grid">
            {state.data.items.map((project) => (
              <li key={project.id}>
                <ProjectCard project={project} />
              </li>
            ))}
          </ul>
          <div className="pager">
            <span>
              Showing {state.data.items.length} of {state.data.totalItems} projects
            </span>
            {state.data.totalPages > 1 && (
              <nav aria-label="Pages">
                <Button variant="secondary" disabled={page === 0} onClick={() => setPage(page - 1)}>
                  Previous
                </Button>
                <span>
                  Page {page + 1} of {state.data.totalPages}
                </span>
                <Button
                  variant="secondary"
                  disabled={page + 1 >= state.data.totalPages}
                  onClick={() => setPage(page + 1)}
                >
                  Next
                </Button>
              </nav>
            )}
          </div>
        </>
      )}

      {creating && (
        <ProjectFormDialog
          title="New project"
          submitLabel="Create project"
          onSubmit={handleCreate}
          onClose={() => setCreating(false)}
        />
      )}
    </>
  );
}

import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import type { Page, Project } from "../../api/projects";
import {
  createTask,
  listTasks,
  type Task,
  type TaskFilters,
  type TaskInput,
  type TaskPriority,
  type TaskStatus,
} from "../../api/tasks";
import { Alert } from "../../components/ui/Alert";
import { Button } from "../../components/ui/Button";
import { SelectField } from "../../components/ui/SelectField";
import { PRIORITY_LABELS, PRIORITY_OPTIONS, STATUS_LABELS, STATUS_OPTIONS } from "./taskLabels";
import { TaskFormDialog } from "./TaskFormDialog";

type LoadState = { status: "loading" } | { status: "error" } | { status: "ready"; data: Page<Task> };

type TasksSectionProps = {
  project: Project;
  /** Called after the tasks changed, so the page can refresh the counts and progress. */
  onChanged: () => void;
};

const ALL_STATUSES = [{ value: "", label: "All statuses" }, ...STATUS_OPTIONS];
const ALL_PRIORITIES = [{ value: "", label: "All priorities" }, ...PRIORITY_OPTIONS];

/** The tasks of a project: filters, the list, paging, and "New task" for Owners and Members. */
export function TasksSection({ project, onChanged }: TasksSectionProps) {
  const [filters, setFilters] = useState<TaskFilters>({});
  const [page, setPage] = useState(0);
  const [reloadKey, setReloadKey] = useState(0);
  const [state, setState] = useState<LoadState>({ status: "loading" });
  const [creating, setCreating] = useState(false);
  const canWrite = project.myRole !== "VIEWER";
  const filtering = Boolean(filters.status || filters.priority);

  useEffect(() => {
    let cancelled = false;
    setState({ status: "loading" });
    listTasks(project.id, filters, page)
      .then((data) => !cancelled && setState({ status: "ready", data }))
      .catch(() => !cancelled && setState({ status: "error" }));
    return () => {
      cancelled = true;
    };
  }, [project.id, filters, page, reloadKey]);

  function changeFilters(next: TaskFilters) {
    setFilters(next);
    setPage(0);
  }

  async function handleCreate(input: TaskInput) {
    await createTask(project.id, input);
    setCreating(false);
    setPage(0); // the new task is the newest, so it is on the first page
    setReloadKey((key) => key + 1);
    onChanged();
  }

  return (
    <section className="tasks" aria-labelledby="tasks-heading">
      <div className="page-heading">
        <h2 id="tasks-heading">Tasks</h2>
        {canWrite && <Button onClick={() => setCreating(true)}>New task</Button>}
      </div>

      <div className="filters">
        <SelectField
          label="Status"
          options={ALL_STATUSES}
          value={filters.status ?? ""}
          onChange={(event) => changeFilters({ ...filters, status: (event.target.value || undefined) as TaskStatus | undefined })}
        />
        <SelectField
          label="Priority"
          options={ALL_PRIORITIES}
          value={filters.priority ?? ""}
          onChange={(event) =>
            changeFilters({ ...filters, priority: (event.target.value || undefined) as TaskPriority | undefined })
          }
        />
        {filtering && (
          <Button variant="secondary" onClick={() => changeFilters({})}>
            Clear filters
          </Button>
        )}
      </div>

      {state.status === "loading" && <p role="status">Loading tasks…</p>}

      {state.status === "error" && (
        <Alert tone="error">
          <p className="alert__text">Could not load the tasks.</p>
          <Button variant="secondary" onClick={() => setReloadKey((key) => key + 1)}>
            Try again
          </Button>
        </Alert>
      )}

      {state.status === "ready" && state.data.totalItems === 0 && (
        <div className="empty">
          <p>{filtering ? "No tasks match the filters." : "No tasks yet."}</p>
          {filtering && (
            <Button variant="secondary" onClick={() => changeFilters({})}>
              Clear filters
            </Button>
          )}
          {!filtering && canWrite && <Button onClick={() => setCreating(true)}>Create the first task</Button>}
        </div>
      )}

      {state.status === "ready" && state.data.totalItems > 0 && (
        <>
          <table className="task-table">
            <thead>
              <tr>
                <th scope="col">Title</th>
                <th scope="col">Status</th>
                <th scope="col">Priority</th>
                <th scope="col">Assignees</th>
              </tr>
            </thead>
            <tbody>
              {state.data.items.map((task) => (
                <tr key={task.id}>
                  <td>
                    <Link to={`/tasks/${task.id}`}>{task.title}</Link>
                  </td>
                  <td>{STATUS_LABELS[task.status]}</td>
                  <td>{PRIORITY_LABELS[task.priority]}</td>
                  <td>
                    {task.assignees.length === 0 ? (
                      <span className="muted">Unassigned</span>
                    ) : (
                      task.assignees.map((person) => person.name).join(", ")
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="pager">
            <span>
              Showing {state.data.items.length} of {state.data.totalItems} tasks
            </span>
            {state.data.totalPages > 1 && (
              <nav aria-label="Task pages">
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
        <TaskFormDialog
          title="New task"
          submitLabel="Create task"
          onSubmit={handleCreate}
          onClose={() => setCreating(false)}
        />
      )}
    </section>
  );
}

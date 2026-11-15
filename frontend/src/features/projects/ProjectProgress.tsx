import type { Project } from "../../api/projects";
import { ProgressBar } from "../../components/ui/ProgressBar";
import { progressLabel } from "./projectLabels";

/** Task counts per status and the progress bar, as on the dashboard card and the project page. */
export function ProjectProgress({ project }: { project: Project }) {
  const { taskCounts, progress } = project;
  const label = `Progress: ${progressLabel(progress)}`;

  return (
    <div className="project-progress">
      <ul className="counts" aria-label="Tasks per status">
        <li>
          <span className="counts__label">To do</span>
          <strong>{taskCounts.todo}</strong>
        </li>
        <li>
          <span className="counts__label">In progress</span>
          <strong>{taskCounts.inProgress}</strong>
        </li>
        <li>
          <span className="counts__label">Done</span>
          <strong>{taskCounts.done}</strong>
        </li>
      </ul>
      <p className="project-progress__label">{label}</p>
      <ProgressBar value={progress.effective} label={label} />
      {progress.manual !== null && (
        <p className="project-progress__note">Computed from tasks: {progress.computed}%</p>
      )}
    </div>
  );
}

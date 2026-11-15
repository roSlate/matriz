import { Link } from "react-router-dom";
import type { Project } from "../../api/projects";
import { Badge } from "../../components/ui/Badge";
import { Card } from "../../components/ui/Card";
import { ProjectProgress } from "./ProjectProgress";
import { ROLE_LABELS } from "./projectLabels";

/** One project on the dashboard. The whole card is a link to the project (AC-06.4). */
export function ProjectCard({ project }: { project: Project }) {
  return (
    <Link to={`/projects/${project.id}`} className="project-card-link">
      <Card className="project-card">
        <h2>{project.name}</h2>
        <Badge>{ROLE_LABELS[project.myRole]}</Badge>
        <ProjectProgress project={project} />
      </Card>
    </Link>
  );
}

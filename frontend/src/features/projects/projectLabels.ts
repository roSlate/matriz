import type { Progress, Role } from "../../api/projects";

export const ROLE_LABELS: Record<Role, string> = {
  OWNER: "Owner",
  MEMBER: "Member",
  VIEWER: "Viewer",
};

/** "50%", or "80% (manual)" when the Owner has set the value by hand. */
export function progressLabel(progress: Progress): string {
  return progress.manual === null ? `${progress.effective}%` : `${progress.effective}% (manual)`;
}

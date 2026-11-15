import { apiRequest } from "./client";

export type Role = "OWNER" | "MEMBER" | "VIEWER";

export type TaskCounts = { todo: number; inProgress: number; done: number; total: number };

export type Progress = { computed: number; manual: number | null; effective: number };

export type Project = {
  id: number;
  name: string;
  description: string | null;
  myRole: Role;
  taskCounts: TaskCounts;
  progress: Progress;
  createdAt: string;
  updatedAt: string;
};

export type Page<T> = { items: T[]; page: number; size: number; totalItems: number; totalPages: number };

export type ProjectInput = { name: string; description: string };

/** Projects shown per page on the dashboard. */
export const PROJECTS_PER_PAGE = 12;

/** The user's projects, newest first; with {@code search}, only those whose name contains it. */
export function listProjects(page: number, search = ""): Promise<Page<Project>> {
  const query = new URLSearchParams({ page: String(page), size: String(PROJECTS_PER_PAGE) });
  if (search.trim() !== "") {
    query.set("search", search.trim());
  }
  return apiRequest<Page<Project>>(`/projects?${query}`);
}

export function getProject(id: number): Promise<Project> {
  return apiRequest<Project>(`/projects/${id}`);
}

export function createProject(input: ProjectInput): Promise<Project> {
  return apiRequest<Project>("/projects", { method: "POST", body: input });
}

export function updateProject(id: number, input: Partial<ProjectInput>): Promise<Project> {
  return apiRequest<Project>(`/projects/${id}`, { method: "PATCH", body: input });
}

/** Permanently deletes the project and everything inside it. Only the Owner may. */
export function deleteProject(id: number): Promise<void> {
  return apiRequest<void>(`/projects/${id}`, { method: "DELETE" });
}

/** Sets the manual progress (a whole number from 0 to 100). Only the Owner may. */
export function setProgressOverride(id: number, percentage: number): Promise<Project> {
  return apiRequest<Project>(`/projects/${id}/progress-override`, { method: "PUT", body: { percentage } });
}

/** Clears the manual progress, so progress is computed from the tasks again. Only the Owner may. */
export function clearProgressOverride(id: number): Promise<Project> {
  return apiRequest<Project>(`/projects/${id}/progress-override`, { method: "DELETE" });
}

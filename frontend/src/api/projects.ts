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

export function listProjects(page: number): Promise<Page<Project>> {
  return apiRequest<Page<Project>>(`/projects?page=${page}&size=${PROJECTS_PER_PAGE}`);
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

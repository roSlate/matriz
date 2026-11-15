import { apiRequest } from "./client";
import type { Page } from "./projects";

export type TaskStatus = "TODO" | "IN_PROGRESS" | "DONE";
export type TaskPriority = "LOW" | "MEDIUM" | "HIGH";

export type Assignee = { id: number; name: string; email: string };

export type Task = {
  id: number;
  projectId: number;
  title: string;
  description: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  assignees: Assignee[];
  createdAt: string;
  updatedAt: string;
};

export type TaskInput = {
  title: string;
  description: string;
  status: TaskStatus;
  priority: TaskPriority;
};

export type TaskFilters = { status?: TaskStatus; priority?: TaskPriority };

/** Tasks shown per page on the project page. */
export const TASKS_PER_PAGE = 10;

export function listTasks(projectId: number, filters: TaskFilters, page: number): Promise<Page<Task>> {
  const query = new URLSearchParams({ page: String(page), size: String(TASKS_PER_PAGE) });
  if (filters.status) {
    query.set("status", filters.status);
  }
  if (filters.priority) {
    query.set("priority", filters.priority);
  }
  return apiRequest<Page<Task>>(`/projects/${projectId}/tasks?${query}`);
}

export function getTask(id: number): Promise<Task> {
  return apiRequest<Task>(`/tasks/${id}`);
}

export function createTask(projectId: number, input: TaskInput): Promise<Task> {
  return apiRequest<Task>(`/projects/${projectId}/tasks`, { method: "POST", body: input });
}

export function updateTask(id: number, input: Partial<TaskInput>): Promise<Task> {
  return apiRequest<Task>(`/tasks/${id}`, { method: "PATCH", body: input });
}

export function deleteTask(id: number): Promise<void> {
  return apiRequest<void>(`/tasks/${id}`, { method: "DELETE" });
}

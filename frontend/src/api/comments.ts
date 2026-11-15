import { apiRequest } from "./client";
import type { Page } from "./projects";

/** The author of a comment: name only, never an email. */
export type CommentAuthor = { id: number; name: string };

export type Comment = {
  id: number;
  taskId: number;
  author: CommentAuthor;
  content: string;
  createdAt: string;
};

/** Comments shown per page; "Show more" loads the next page. */
export const COMMENTS_PER_PAGE = 20;

/** One page of a task's comments, oldest first. */
export function listComments(taskId: number, page: number): Promise<Page<Comment>> {
  const query = new URLSearchParams({ page: String(page), size: String(COMMENTS_PER_PAGE) });
  return apiRequest<Page<Comment>>(`/tasks/${taskId}/comments?${query}`);
}

export function addComment(taskId: number, content: string): Promise<Comment> {
  return apiRequest<Comment>(`/tasks/${taskId}/comments`, { method: "POST", body: { content } });
}

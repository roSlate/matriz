import { apiRequest } from "./client";
import type { Role } from "./projects";

export type Member = { userId: number; name: string; email: string; role: Role; addedAt: string };

/** The roles the Owner can give; the Owner role belongs to the project's creator only. */
export type AssignableRole = "MEMBER" | "VIEWER";

export type MemberInput = { email: string; role: AssignableRole };

/** The project's members, Owner first. Every member of the project may read the list. */
export function listMembers(projectId: number): Promise<Member[]> {
  return apiRequest<Member[]>(`/projects/${projectId}/members`);
}

export function addMember(projectId: number, input: MemberInput): Promise<Member> {
  return apiRequest<Member>(`/projects/${projectId}/members`, { method: "POST", body: input });
}

export function changeMemberRole(projectId: number, userId: number, role: AssignableRole): Promise<Member> {
  return apiRequest<Member>(`/projects/${projectId}/members/${userId}`, { method: "PATCH", body: { role } });
}

export function removeMember(projectId: number, userId: number): Promise<void> {
  return apiRequest<void>(`/projects/${projectId}/members/${userId}`, { method: "DELETE" });
}

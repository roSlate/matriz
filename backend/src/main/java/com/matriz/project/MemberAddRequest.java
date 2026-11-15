package com.matriz.project;

/** Body of {@code POST /api/v1/projects/{projectId}/members}: an existing user's email and the role to give. */
public record MemberAddRequest(String email, Role role) {
}

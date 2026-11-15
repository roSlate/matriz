package com.matriz.identity;

/** A user as shown next to a task or a project: id, name and email, never the password. */
public record UserSummary(Long id, String name, String email) {
}

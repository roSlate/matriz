package com.matriz.project;

/**
 * Body of {@code PATCH /api/v1/projects/{projectId}}. A missing or null field is left unchanged;
 * to clear the description, send an empty string.
 */
public record ProjectUpdateRequest(String name, String description) {
}

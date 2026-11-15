package com.matriz.project;

/**
 * Body of {@code POST /api/v1/projects}. The owner is never part of it: the server takes it from
 * the session, and any other field is refused (see openapi.yaml). Limits are checked on the trimmed
 * text by the service.
 */
public record ProjectCreateRequest(String name, String description) {
}

package com.matriz.task;

/**
 * Body of {@code POST /api/v1/projects/{projectId}/tasks}. Status and priority are optional and
 * default to {@code TODO} and {@code MEDIUM}; any other field is refused (see openapi.yaml).
 */
public record TaskCreateRequest(String title, String description, TaskStatus status, TaskPriority priority) {
}

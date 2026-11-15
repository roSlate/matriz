package com.matriz.task;

/**
 * Body of {@code PATCH /api/v1/tasks/{taskId}}. A missing or null field is left unchanged; to clear
 * the description, send an empty string.
 */
public record TaskUpdateRequest(String title, String description, TaskStatus status, TaskPriority priority) {
}

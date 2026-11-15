package com.matriz.task;

/** Body of {@code POST /api/v1/tasks/{taskId}/assignees}: the user to assign. */
public record AssigneeAddRequest(Long userId) {
}

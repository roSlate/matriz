package com.matriz.task;

import com.matriz.identity.UserSummary;
import java.time.Instant;
import java.util.List;

/** A task as returned by the API, with the people assigned to it (an empty list when it is unassigned). */
public record TaskResponse(
        Long id,
        Long projectId,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        List<UserSummary> assignees,
        Instant createdAt,
        Instant updatedAt) {

    static TaskResponse of(Task task, List<UserSummary> assignees) {
        return new TaskResponse(task.getId(), task.getProjectId(), task.getTitle(), task.getDescription(),
                task.getStatus(), task.getPriority(), assignees, task.getCreatedAt(), task.getUpdatedAt());
    }
}

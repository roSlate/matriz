package com.matriz.project;

import java.time.Instant;

/** A project as returned by the API, with the caller's role, the task counts and the progress. */
public record ProjectResponse(
        Long id,
        String name,
        String description,
        Role myRole,
        TaskCountsResponse taskCounts,
        Progress progress,
        Instant createdAt,
        Instant updatedAt) {

    /** Task counts per status; {@code total} is always the sum of the three. */
    public record TaskCountsResponse(int todo, int inProgress, int done, int total) {
    }

    static ProjectResponse of(Project project, Role role, TaskCounts.Counts counts) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                role,
                new TaskCountsResponse(counts.todo(), counts.inProgress(), counts.done(), counts.total()),
                Progress.of(counts, project.getProgressOverride()),
                project.getCreatedAt(),
                project.getUpdatedAt());
    }
}

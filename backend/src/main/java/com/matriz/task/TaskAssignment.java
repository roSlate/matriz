package com.matriz.task;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A user assigned to a task (table {@code task_assignments}). The database guarantees that the
 * user is a member of the task's project (docs/database.md, section 3). In this increment
 * assignments are only read; the operations that create and remove them come in increment 5.
 */
@Entity
@Table(name = "task_assignments")
public class TaskAssignment {

    @EmbeddedId
    private TaskAssignmentKey id;

    @Column(name = "project_id", nullable = false, updatable = false)
    private Long projectId;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    protected TaskAssignment() {
        // required by JPA
    }

    public TaskAssignmentKey getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }
}

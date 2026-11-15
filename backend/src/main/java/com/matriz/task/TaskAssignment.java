package com.matriz.task;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A user assigned to a task (table {@code task_assignments}). The database guarantees that the
 * user is a member of the task's project (docs/database.md, section 3). The rules for creating
 * and removing assignments are in {@link AssigneeService}.
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

    public TaskAssignment(long taskId, long userId, long projectId) {
        this.id = new TaskAssignmentKey(taskId, userId);
        this.projectId = projectId;
        this.assignedAt = Instant.now();
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

package com.matriz.task;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A task (table {@code tasks}, see docs/database.md). It belongs to exactly one project, which is
 * kept as a plain identifier: the task module does not depend on the project module's entities.
 */
@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false, updatable = false)
    private Long projectId;

    @Column(nullable = false)
    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskPriority priority;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Task() {
        // required by JPA
    }

    /** Creates a task that has not been saved yet. Text arrives already trimmed, with empty text as null. */
    public Task(Long projectId, String title, String description, TaskStatus status, TaskPriority priority) {
        this.projectId = projectId;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /**
     * Changes the task. A null argument leaves that field as it is. The description is replaced only
     * when {@code changeDescription} is true, and then {@code newDescription} may be null to clear it.
     */
    public void update(String newTitle, boolean changeDescription, String newDescription,
                       TaskStatus newStatus, TaskPriority newPriority) {
        if (newTitle != null) {
            this.title = newTitle;
        }
        if (changeDescription) {
            this.description = newDescription;
        }
        if (newStatus != null) {
            this.status = newStatus;
        }
        if (newPriority != null) {
            this.priority = newPriority;
        }
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

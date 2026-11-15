package com.matriz.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A project (table {@code projects}, see docs/database.md). The Owner is a membership row, not a column. */
@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "progress_override")
    private Short progressOverride;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Project() {
        // required by JPA
    }

    /**
     * Creates a project that has not been saved yet. The caller passes the already trimmed
     * name and a description that is null when empty.
     */
    public Project(String name, String description) {
        this.name = name;
        this.description = description;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /**
     * Changes the project. A null {@code newName} leaves the name as it is. The description is
     * replaced only when {@code changeDescription} is true, and then {@code newDescription} may be
     * null to clear it.
     */
    public void update(String newName, boolean changeDescription, String newDescription) {
        if (newName != null) {
            this.name = newName;
        }
        if (changeDescription) {
            this.description = newDescription;
        }
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    /** The Owner's manual progress (0 to 100), or null when progress is computed from the tasks. */
    public Integer getProgressOverride() {
        return progressOverride == null ? null : progressOverride.intValue();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

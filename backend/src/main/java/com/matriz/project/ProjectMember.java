package com.matriz.project;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A user's membership of a project and their role in it (table {@code project_members}). The
 * Owner is the membership with role {@code OWNER}; a project has exactly one.
 */
@Entity
@Table(name = "project_members")
public class ProjectMember {

    @EmbeddedId
    private ProjectMemberKey id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("projectId")
    @JoinColumn(name = "project_id")
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt;

    protected ProjectMember() {
        // required by JPA
    }

    /** The project must already be saved, so that it has an identifier. */
    public ProjectMember(Project project, Long userId, Role role) {
        this.id = new ProjectMemberKey(project.getId(), userId);
        this.project = project;
        this.role = role;
        this.addedAt = Instant.now();
    }

    public ProjectMemberKey getId() {
        return id;
    }

    public Project getProject() {
        return project;
    }

    public Role getRole() {
        return role;
    }

    public void changeRole(Role newRole) {
        this.role = newRole;
    }

    public Instant getAddedAt() {
        return addedAt;
    }
}

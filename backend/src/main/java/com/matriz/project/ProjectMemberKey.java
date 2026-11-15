package com.matriz.project;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** Primary key of a membership: the project and the user together. */
@Embeddable
public class ProjectMemberKey implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "user_id")
    private Long userId;

    protected ProjectMemberKey() {
        // required by JPA
    }

    public ProjectMemberKey(Long projectId, Long userId) {
        this.projectId = projectId;
        this.userId = userId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ProjectMemberKey key
                && Objects.equals(projectId, key.projectId) && Objects.equals(userId, key.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectId, userId);
    }
}

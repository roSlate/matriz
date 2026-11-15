package com.matriz.project;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Database access for {@link ProjectMember}. Used by the project module only. */
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberKey> {

    /** The memberships of one user, with their projects loaded, so a page needs a single query. */
    @EntityGraph(attributePaths = "project")
    Page<ProjectMember> findByIdUserId(Long userId, Pageable pageable);

    /**
     * The same, only for projects whose name contains the given text, ignoring letter case. Spring
     * Data escapes {@code %} and {@code _} here, so the user's text is always matched literally.
     */
    @EntityGraph(attributePaths = "project")
    Page<ProjectMember> findByIdUserIdAndProjectNameContainingIgnoreCase(Long userId, String name, Pageable pageable);
}

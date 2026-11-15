package com.matriz.project;

import org.springframework.data.jpa.repository.JpaRepository;

/** Database access for {@link Project}. Used by the project module only. */
public interface ProjectRepository extends JpaRepository<Project, Long> {
}

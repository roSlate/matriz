package com.matriz.task;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Database access for {@link Task}. Used by the task module only. */
public interface TaskRepository extends JpaRepository<Task, Long> {

    Page<Task> findByProjectId(Long projectId, Pageable pageable);

    Page<Task> findByProjectIdAndStatus(Long projectId, TaskStatus status, Pageable pageable);

    Page<Task> findByProjectIdAndPriority(Long projectId, TaskPriority priority, Pageable pageable);

    Page<Task> findByProjectIdAndStatusAndPriority(Long projectId, TaskStatus status, TaskPriority priority,
                                                   Pageable pageable);

    /** The project a task belongs to; used by the access step before anything else is loaded. */
    @Query("select t.projectId from Task t where t.id = :taskId")
    Optional<Long> findProjectIdById(@Param("taskId") Long taskId);

    /** Rows of project id, status and number of tasks, for several projects at once. */
    @Query("select t.projectId, t.status, count(t) from Task t where t.projectId in :projectIds "
            + "group by t.projectId, t.status")
    List<Object[]> countByProjectAndStatus(@Param("projectIds") Collection<Long> projectIds);
}

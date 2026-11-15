package com.matriz.task;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Database access for {@link TaskAssignment}. Used by the task module only. */
public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, TaskAssignmentKey> {

    /** The assignments of several tasks at once, so a page of tasks needs one query. */
    List<TaskAssignment> findByIdTaskIdIn(Collection<Long> taskIds);

    /** Deletes one user's assignments in one project, whichever tasks they are on. */
    @Modifying
    @Query("delete from TaskAssignment a where a.projectId = :projectId and a.id.userId = :userId")
    void deleteByProjectAndUser(@Param("projectId") long projectId, @Param("userId") long userId);
}

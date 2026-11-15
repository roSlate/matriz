package com.matriz.task;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Database access for {@link TaskAssignment}. Used by the task module only. */
public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, TaskAssignmentKey> {

    /** The assignments of several tasks at once, so a page of tasks needs one query. */
    List<TaskAssignment> findByIdTaskIdIn(Collection<Long> taskIds);
}

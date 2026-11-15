package com.matriz.task;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** Primary key of an assignment: the task and the user together. */
@Embeddable
public class TaskAssignmentKey implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "user_id")
    private Long userId;

    protected TaskAssignmentKey() {
        // required by JPA
    }

    public TaskAssignmentKey(Long taskId, Long userId) {
        this.taskId = taskId;
        this.userId = userId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TaskAssignmentKey key
                && Objects.equals(taskId, key.taskId) && Objects.equals(userId, key.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, userId);
    }
}

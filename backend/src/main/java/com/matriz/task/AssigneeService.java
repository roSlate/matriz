package com.matriz.task;

import com.matriz.common.ApiException;
import com.matriz.common.FieldValidation;
import com.matriz.project.ProjectAccess;
import com.matriz.project.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Assigning people to tasks (US-07). The caller's role in the task's project was found by the
 * task access step, which answered {@code 404} to outsiders. The order of checks then is: input
 * ({@code 400}), the caller's role ({@code 403}), the rules of the operation ({@code 422},
 * {@code 409}, {@code 404}). The database still guarantees, as a safety net, that an assignee is
 * a member of the project and that nobody is assigned twice.
 */
@Service
public class AssigneeService {

    private final TaskRepository tasks;
    private final TaskAssignmentRepository assignments;
    private final ProjectAccess access;
    private final TaskService taskService;

    public AssigneeService(TaskRepository tasks, TaskAssignmentRepository assignments, ProjectAccess access,
                           TaskService taskService) {
        this.tasks = tasks;
        this.assignments = assignments;
        this.access = access;
        this.taskService = taskService;
    }

    /** Assigns a user who is an Owner or Member of the task's project, once (AC-07.1, 07.3, 07.4, 07.5). */
    @Transactional
    public TaskResponse assign(long taskId, Role callerRole, AssigneeAddRequest request) {
        if (request.userId() == null || request.userId() < 1) {
            FieldValidation.create().addError("userId", "Must be a positive whole number.").throwIfInvalid();
        }
        ProjectAccess.requireAtLeast(callerRole, Role.MEMBER);

        long projectId = projectOf(taskId);
        long userId = request.userId();
        boolean eligible = access.roleOf(userId, projectId).map(role -> role.atLeast(Role.MEMBER)).orElse(false);
        if (!eligible) {
            throw ApiException.unprocessable(
                    "Only Owners and Members of the project can be assigned.", "USER_NOT_ASSIGNABLE");
        }
        if (assignments.existsById(new TaskAssignmentKey(taskId, userId))) {
            throw ApiException.conflict("This user is already assigned to the task.", "ALREADY_ASSIGNED");
        }
        assignments.save(new TaskAssignment(taskId, userId, projectId));
        assignments.flush();
        return taskService.get(taskId);
    }

    /** Removes one assignment and nothing else (AC-07.2); a user who is not assigned gives {@code 404}. */
    @Transactional
    public void unassign(long taskId, Role callerRole, long userId) {
        ProjectAccess.requireAtLeast(callerRole, Role.MEMBER);

        TaskAssignment assignment = assignments.findById(new TaskAssignmentKey(taskId, userId))
                .orElseThrow(ApiException::notFound);
        assignments.delete(assignment);
    }

    private long projectOf(long taskId) {
        return tasks.findProjectIdById(taskId).orElseThrow(ApiException::notFound);
    }
}

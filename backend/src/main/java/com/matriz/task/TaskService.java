package com.matriz.task;

import com.matriz.common.ApiException;
import com.matriz.common.FieldValidation;
import com.matriz.common.PageResponse;
import com.matriz.identity.UserDirectory;
import com.matriz.identity.UserSummary;
import com.matriz.project.ProjectAccess;
import com.matriz.project.Role;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business rules for tasks: US-04, US-05 and the filters of US-09. The caller's role has already
 * been established by an access step, which answered {@code 404} for outsiders; this class applies
 * the rest of the order of checks (docs/architecture.md, section 5): the input is checked ({@code 400})
 * before the role ({@code 403}).
 */
@Service
public class TaskService {

    private static final int TITLE_MIN = 3;
    private static final int TITLE_MAX = 150;
    private static final int DESCRIPTION_MAX = 2000;

    private final TaskRepository tasks;
    private final TaskAssignmentRepository assignments;
    private final UserDirectory users;

    public TaskService(TaskRepository tasks, TaskAssignmentRepository assignments, UserDirectory users) {
        this.tasks = tasks;
        this.assignments = assignments;
        this.users = users;
    }

    /** The tasks of a project, newest first, optionally only those with a given status and/or priority. */
    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> list(long projectId, TaskStatus status, TaskPriority priority, Pageable pageable) {
        Page<Task> page;
        if (status != null && priority != null) {
            page = tasks.findByProjectIdAndStatusAndPriority(projectId, status, priority, pageable);
        } else if (status != null) {
            page = tasks.findByProjectIdAndStatus(projectId, status, pageable);
        } else if (priority != null) {
            page = tasks.findByProjectIdAndPriority(projectId, priority, pageable);
        } else {
            page = tasks.findByProjectId(projectId, pageable);
        }
        return PageResponse.of(page, responses(page.getContent()));
    }

    @Transactional
    public TaskResponse create(long projectId, Role role, TaskCreateRequest request) {
        FieldValidation.create()
                .requiredText("title", request.title(), TITLE_MIN, TITLE_MAX)
                .optionalText("description", request.description(), DESCRIPTION_MAX)
                .throwIfInvalid();
        ProjectAccess.requireAtLeast(role, Role.MEMBER);

        Task task = tasks.save(new Task(
                projectId,
                FieldValidation.trim(request.title()),
                FieldValidation.blankToNull(request.description()),
                request.status() == null ? TaskStatus.TODO : request.status(),
                request.priority() == null ? TaskPriority.MEDIUM : request.priority()));
        return TaskResponse.of(task, List.of());
    }

    @Transactional(readOnly = true)
    public TaskResponse get(long taskId) {
        return responses(List.of(load(taskId))).get(0);
    }

    @Transactional
    public TaskResponse update(long taskId, Role role, TaskUpdateRequest request) {
        if (request.title() == null && request.description() == null
                && request.status() == null && request.priority() == null) {
            throw ApiException.badRequest("Provide at least one of: title, description, status, priority.");
        }
        FieldValidation validation = FieldValidation.create();
        if (request.title() != null) {
            validation.requiredText("title", request.title(), TITLE_MIN, TITLE_MAX);
        }
        validation.optionalText("description", request.description(), DESCRIPTION_MAX).throwIfInvalid();
        ProjectAccess.requireAtLeast(role, Role.MEMBER);

        Task task = load(taskId);
        task.update(FieldValidation.trim(request.title()), request.description() != null,
                FieldValidation.blankToNull(request.description()), request.status(), request.priority());
        return responses(List.of(task)).get(0);
    }

    /** Deletes a task. The database removes its assignments and comments with it. */
    @Transactional
    public void delete(long taskId, Role role) {
        ProjectAccess.requireAtLeast(role, Role.MEMBER);
        tasks.delete(load(taskId));
    }

    private Task load(long taskId) {
        return tasks.findById(taskId).orElseThrow(ApiException::notFound);
    }

    /** Builds the responses for several tasks with two extra queries in total, not two per task. */
    private List<TaskResponse> responses(Collection<Task> list) {
        List<Long> taskIds = list.stream().map(Task::getId).toList();
        List<TaskAssignment> found = taskIds.isEmpty() ? List.of() : assignments.findByIdTaskIdIn(taskIds);
        Map<Long, UserSummary> people = users.summaries(
                found.stream().map(assignment -> assignment.getId().getUserId()).distinct().toList());

        Map<Long, List<UserSummary>> byTask = new HashMap<>();
        for (TaskAssignment assignment : found) {
            UserSummary person = people.get(assignment.getId().getUserId());
            if (person != null) {
                byTask.computeIfAbsent(assignment.getId().getTaskId(), id -> new java.util.ArrayList<>()).add(person);
            }
        }
        byTask.values().forEach(group -> group.sort(Comparator.comparing(UserSummary::name)));

        return list.stream()
                .map(task -> TaskResponse.of(task, byTask.getOrDefault(task.getId(), List.of())))
                .toList();
    }
}

package com.matriz.task;

import com.matriz.common.PageResponse;
import com.matriz.project.AccessStepInterceptor;
import com.matriz.project.Role;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tasks (US-04, US-05). Routes with a project or task identifier are guarded by an access step
 * before they reach these methods, which receive the caller's role from it.
 */
@RestController
@RequestMapping("/api/v1")
public class TaskController {

    private final TaskService tasks;

    public TaskController(TaskService tasks) {
        this.tasks = tasks;
    }

    @GetMapping("/projects/{projectId}/tasks")
    public PageResponse<TaskResponse> list(@PathVariable long projectId,
                                           @RequestParam(required = false) TaskStatus status,
                                           @RequestParam(required = false) TaskPriority priority,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        Sort newestFirst = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        return tasks.list(projectId, status, priority, PageResponse.pageable(page, size, newestFirst));
    }

    @PostMapping("/projects/{projectId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@PathVariable long projectId,
                               @RequestAttribute(AccessStepInterceptor.ROLE_ATTRIBUTE) Role role,
                               @RequestBody TaskCreateRequest body) {
        return tasks.create(projectId, role, body);
    }

    @GetMapping("/tasks/{taskId}")
    public TaskResponse get(@PathVariable long taskId) {
        return tasks.get(taskId);
    }

    @PatchMapping("/tasks/{taskId}")
    public TaskResponse update(@PathVariable long taskId,
                               @RequestAttribute(AccessStepInterceptor.ROLE_ATTRIBUTE) Role role,
                               @RequestBody TaskUpdateRequest body) {
        return tasks.update(taskId, role, body);
    }

    @DeleteMapping("/tasks/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long taskId,
                       @RequestAttribute(AccessStepInterceptor.ROLE_ATTRIBUTE) Role role) {
        tasks.delete(taskId, role);
    }
}

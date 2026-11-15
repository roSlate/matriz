package com.matriz.task;

import com.matriz.common.Identifiers;
import com.matriz.project.AccessStepInterceptor;
import com.matriz.project.Role;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Assignees of a task (US-07), guarded by the task access step like every route with a task identifier. */
@RestController
@RequestMapping("/api/v1/tasks/{taskId}/assignees")
public class AssigneeController {

    private final AssigneeService assignees;

    public AssigneeController(AssigneeService assignees) {
        this.assignees = assignees;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse assign(@PathVariable long taskId,
                               @RequestAttribute(AccessStepInterceptor.ROLE_ATTRIBUTE) Role role,
                               @RequestBody AssigneeAddRequest body) {
        return assignees.assign(taskId, role, body);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassign(@PathVariable long taskId, @PathVariable String userId,
                         @RequestAttribute(AccessStepInterceptor.ROLE_ATTRIBUTE) Role role) {
        assignees.unassign(taskId, role, Identifiers.parse(userId));
    }
}

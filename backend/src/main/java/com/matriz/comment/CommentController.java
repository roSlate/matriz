package com.matriz.comment;

import com.matriz.common.PageResponse;
import com.matriz.identity.UserPrincipal;
import com.matriz.project.AccessStepInterceptor;
import com.matriz.project.Role;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Comments on a task (US-08). Routes with a task identifier are guarded by the task access step
 * before they reach these methods, which receive the caller's role from it.
 */
@RestController
@RequestMapping("/api/v1/tasks/{taskId}/comments")
public class CommentController {

    private final CommentService comments;

    public CommentController(CommentService comments) {
        this.comments = comments;
    }

    /** The task's comments, oldest first. */
    @GetMapping
    public PageResponse<CommentResponse> list(@PathVariable long taskId,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        Sort oldestFirst = Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id"));
        return comments.list(taskId, PageResponse.pageable(page, size, oldestFirst));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse add(@PathVariable long taskId, @AuthenticationPrincipal UserPrincipal user,
                               @RequestAttribute(AccessStepInterceptor.ROLE_ATTRIBUTE) Role role,
                               @RequestBody CommentCreateRequest body) {
        return comments.add(taskId, user.id(), role, body);
    }
}

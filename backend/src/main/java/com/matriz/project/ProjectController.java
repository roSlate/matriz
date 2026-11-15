package com.matriz.project;

import com.matriz.common.PageResponse;
import com.matriz.identity.UserPrincipal;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
 * Projects (US-02). Routes with a {@code projectId} are guarded by the {@link AccessStepInterceptor}
 * before they reach these methods, which receive the caller's role from it.
 */
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projects;

    public ProjectController(ProjectService projects) {
        this.projects = projects;
    }

    /** The projects the caller is a member of, newest first, optionally only those whose name contains {@code search}. */
    @GetMapping
    public PageResponse<ProjectResponse> list(@AuthenticationPrincipal UserPrincipal user,
                                              @RequestParam(required = false) String search,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        Sort newestFirst = Sort.by(Sort.Order.desc("project.createdAt"), Sort.Order.desc("project.id"));
        return projects.list(user.id(), search, PageResponse.pageable(page, size, newestFirst));
    }

    /** Creates a project; the caller, taken from the session, becomes its Owner. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(@AuthenticationPrincipal UserPrincipal user,
                                  @RequestBody ProjectCreateRequest body) {
        return projects.create(user.id(), body);
    }

    @GetMapping("/{projectId}")
    public ProjectResponse get(@PathVariable long projectId,
                               @RequestAttribute(AccessStepInterceptor.ROLE_ATTRIBUTE) Role role) {
        return projects.get(projectId, role);
    }

    @PatchMapping("/{projectId}")
    public ProjectResponse update(@PathVariable long projectId,
                                  @RequestAttribute(AccessStepInterceptor.ROLE_ATTRIBUTE) Role role,
                                  @RequestBody ProjectUpdateRequest body) {
        return projects.update(projectId, role, body);
    }

    /** Permanently deletes the project and everything inside it. Owner only. */
    @DeleteMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long projectId,
                       @RequestAttribute(AccessStepInterceptor.ROLE_ATTRIBUTE) Role role) {
        projects.delete(projectId, role);
    }
}

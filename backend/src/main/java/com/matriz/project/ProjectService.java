package com.matriz.project;

import com.matriz.common.ApiException;
import com.matriz.common.FieldValidation;
import com.matriz.common.PageResponse;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business rules for projects: US-02, and the project part of US-06. */
@Service
public class ProjectService {

    private static final int NAME_MIN = 3;
    private static final int NAME_MAX = 100;
    private static final int DESCRIPTION_MAX = 1000;
    private static final int SEARCH_MAX = 100;

    private final ProjectRepository projects;
    private final ProjectMemberRepository members;
    private final TaskCounts taskCounts;

    public ProjectService(ProjectRepository projects, ProjectMemberRepository members, TaskCounts taskCounts) {
        this.projects = projects;
        this.members = members;
        this.taskCounts = taskCounts;
    }

    /**
     * Creates a project and makes the caller its Owner, in one transaction, so a project can never
     * exist without an Owner (FR-05, AC-02.1).
     */
    @Transactional
    public ProjectResponse create(long userId, ProjectCreateRequest request) {
        FieldValidation.create()
                .requiredText("name", request.name(), NAME_MIN, NAME_MAX)
                .optionalText("description", request.description(), DESCRIPTION_MAX)
                .throwIfInvalid();

        Project project = projects.save(new Project(
                FieldValidation.trim(request.name()), FieldValidation.blankToNull(request.description())));
        members.save(new ProjectMember(project, userId, Role.OWNER));
        return ProjectResponse.of(project, Role.OWNER, TaskCounts.Counts.NONE);
    }

    /**
     * The projects the user is a member of, newest first, with their counts and progress. If
     * {@code search} has text after trimming, only projects whose name contains it (ignoring letter
     * case) are returned (FR-09).
     */
    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> list(long userId, String search, Pageable pageable) {
        FieldValidation.create().optionalText("search", search, SEARCH_MAX).throwIfInvalid();
        String text = FieldValidation.blankToNull(search);
        Page<ProjectMember> page = text == null
                ? members.findByIdUserId(userId, pageable)
                : members.findByIdUserIdAndProjectNameContainingIgnoreCase(userId, text, pageable);
        List<Long> ids = page.getContent().stream().map(member -> member.getProject().getId()).toList();
        Map<Long, TaskCounts.Counts> counts = taskCounts.countsFor(ids);

        List<ProjectResponse> items = page.getContent().stream()
                .map(member -> ProjectResponse.of(member.getProject(), member.getRole(),
                        counts.getOrDefault(member.getProject().getId(), TaskCounts.Counts.NONE)))
                .toList();
        return PageResponse.of(page, items);
    }

    /** One project, for a user whose role the access step has already established. */
    @Transactional(readOnly = true)
    public ProjectResponse get(long projectId, Role role) {
        return response(load(projectId), role);
    }

    /**
     * Changes the name and/or the description (FR-07). Only the Owner may. The input is checked
     * before the role, matching the order of checks in docs/architecture.md: 404, then 400, then 403.
     */
    @Transactional
    public ProjectResponse update(long projectId, Role role, ProjectUpdateRequest request) {
        if (request.name() == null && request.description() == null) {
            throw ApiException.badRequest("Provide at least one of: name, description.");
        }
        FieldValidation validation = FieldValidation.create();
        if (request.name() != null) {
            validation.requiredText("name", request.name(), NAME_MIN, NAME_MAX);
        }
        validation.optionalText("description", request.description(), DESCRIPTION_MAX).throwIfInvalid();
        ProjectAccess.requireAtLeast(role, Role.OWNER);

        Project project = load(projectId);
        project.update(FieldValidation.trim(request.name()), request.description() != null,
                FieldValidation.blankToNull(request.description()));
        return response(project, role);
    }

    /**
     * Deletes a project (FR-08). Only the Owner may. The database removes everything inside it, so no
     * orphaned data is left: memberships, tasks, assignments and comments. Users are not touched.
     */
    @Transactional
    public void delete(long projectId, Role role) {
        ProjectAccess.requireAtLeast(role, Role.OWNER);
        projects.delete(load(projectId));
    }

    private Project load(long projectId) {
        return projects.findById(projectId).orElseThrow(ApiException::notFound);
    }

    private ProjectResponse response(Project project, Role role) {
        TaskCounts.Counts counts = taskCounts.countsFor(List.of(project.getId()))
                .getOrDefault(project.getId(), TaskCounts.Counts.NONE);
        return ProjectResponse.of(project, role, counts);
    }
}

package com.matriz.project;

import com.matriz.common.ApiException;
import com.matriz.common.FieldValidation;
import com.matriz.identity.UserDirectory;
import com.matriz.identity.UserSummary;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business rules for members and roles (US-03). Every method receives the caller's role from the
 * access step. As in the rest of the API the order of checks is: 404 (no access), 400 (invalid
 * input), 403 (role too low), then the rules of the operation (404, 409, 422).
 */
@Service
public class MemberService {

    private static final int EMAIL_MAX = 254;
    private static final Set<Role> ASSIGNABLE = Set.of(Role.MEMBER, Role.VIEWER);

    private final ProjectRepository projects;
    private final ProjectMemberRepository members;
    private final UserDirectory users;
    private final ApplicationEventPublisher events;

    public MemberService(ProjectRepository projects, ProjectMemberRepository members, UserDirectory users,
                         ApplicationEventPublisher events) {
        this.projects = projects;
        this.members = members;
        this.users = users;
        this.events = events;
    }

    /** The members of a project, Owner first, then by name. Any member may read the list (AC-03.9). */
    @Transactional(readOnly = true)
    public List<MemberResponse> list(long projectId) {
        List<ProjectMember> found = members.findByIdProjectId(projectId);
        Map<Long, UserSummary> summaries =
                users.summaries(found.stream().map(member -> member.getId().getUserId()).toList());
        return found.stream()
                .filter(member -> summaries.containsKey(member.getId().getUserId()))
                .map(member -> MemberResponse.of(member, summaries.get(member.getId().getUserId())))
                .sorted(Comparator.comparing((MemberResponse member) -> member.role() != Role.OWNER)
                        .thenComparing(member -> member.name().toLowerCase())
                        .thenComparing(MemberResponse::userId))
                .toList();
    }

    /** Adds an existing user, found by email, as Member or Viewer (AC-03.1, 03.4, 03.7, 03.9). */
    @Transactional
    public MemberResponse add(long projectId, Role callerRole, MemberAddRequest request) {
        FieldValidation validation = FieldValidation.create()
                .requiredText("email", request.email(), 3, EMAIL_MAX);
        requireAssignable(validation, request.role());
        validation.throwIfInvalid();
        ProjectAccess.requireAtLeast(callerRole, Role.OWNER);

        UserSummary user = users.findByEmail(FieldValidation.trim(request.email()))
                .orElseThrow(() -> ApiException.unprocessable("No user has that email.", "USER_NOT_FOUND"));
        if (members.existsById(new ProjectMemberKey(projectId, user.id()))) {
            throw ApiException.conflict("This user is already a member of the project.", "ALREADY_MEMBER");
        }
        ProjectMember member = members.save(new ProjectMember(loadProject(projectId), user.id(), request.role()));
        return MemberResponse.of(member, user);
    }

    /**
     * Changes a member's role (AC-03.2, 03.6, 03.8). A demotion to Viewer publishes
     * {@link MembershipChanged}, so the task module removes the member's assignments.
     */
    @Transactional
    public MemberResponse changeRole(long projectId, Role callerRole, long userId, MemberRoleChangeRequest request) {
        FieldValidation validation = FieldValidation.create();
        requireAssignable(validation, request.role());
        validation.throwIfInvalid();
        ProjectAccess.requireAtLeast(callerRole, Role.OWNER);

        ProjectMember member = find(projectId, userId);
        refuseOwner(member);
        boolean demoted = member.getRole() != Role.VIEWER && request.role() == Role.VIEWER;
        member.changeRole(request.role());
        if (demoted) {
            events.publishEvent(new MembershipChanged(projectId, userId));
        }
        UserSummary user = users.summaries(List.of(userId)).get(userId);
        return MemberResponse.of(member, user);
    }

    /** Removes a member (AC-03.5, 03.6): their access ends at once and their assignments go. */
    @Transactional
    public void remove(long projectId, Role callerRole, long userId) {
        ProjectAccess.requireAtLeast(callerRole, Role.OWNER);

        ProjectMember member = find(projectId, userId);
        refuseOwner(member);
        members.delete(member);
        events.publishEvent(new MembershipChanged(projectId, userId));
    }

    private ProjectMember find(long projectId, long userId) {
        return members.findById(new ProjectMemberKey(projectId, userId)).orElseThrow(ApiException::notFound);
    }

    /** A project always has exactly one Owner: the Owner cannot be demoted or removed (AC-03.6). */
    private static void refuseOwner(ProjectMember member) {
        if (member.getRole() == Role.OWNER) {
            throw ApiException.conflict("The Owner of a project cannot be changed or removed.", "OWNER_PROTECTED");
        }
    }

    private static void requireAssignable(FieldValidation validation, Role role) {
        if (role == null || !ASSIGNABLE.contains(role)) {
            validation.addError("role", "Must be MEMBER or VIEWER.");
        }
    }

    private Project loadProject(long projectId) {
        return projects.findById(projectId).orElseThrow(ApiException::notFound);
    }
}

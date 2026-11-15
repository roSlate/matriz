package com.matriz.project;

import com.matriz.common.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The access policy of Matriz (docs/architecture.md, section 5): "does this user have at least
 * this role on this project?". Every module that works on data inside a project asks it, so the
 * permission matrix lives in one place.
 */
@Service
public class ProjectAccess {

    private final ProjectMemberRepository members;

    public ProjectAccess(ProjectMemberRepository members) {
        this.members = members;
    }

    /**
     * Returns the user's role in the project. A user who is not a member gets {@code 404}, the same
     * answer as for a project that does not exist, so the existence of a project is never revealed.
     */
    @Transactional(readOnly = true)
    public Role requireMembership(long userId, long projectId) {
        return members.findById(new ProjectMemberKey(projectId, userId))
                .map(ProjectMember::getRole)
                .orElseThrow(ApiException::notFound);
    }

    /** Refuses with {@code 403} when the role is lower than the operation needs. */
    public static void requireAtLeast(Role actual, Role minimum) {
        if (!actual.atLeast(minimum)) {
            throw ApiException.forbidden();
        }
    }
}

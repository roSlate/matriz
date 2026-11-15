package com.matriz.project;

import com.matriz.identity.UserSummary;
import java.time.Instant;

/** A member as the API shows them: name, email and role (the email is visible to project members, CC-01). */
public record MemberResponse(Long userId, String name, String email, Role role, Instant addedAt) {

    static MemberResponse of(ProjectMember member, UserSummary user) {
        return new MemberResponse(user.id(), user.name(), user.email(), member.getRole(), member.getAddedAt());
    }
}

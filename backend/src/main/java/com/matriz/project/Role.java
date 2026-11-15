package com.matriz.project;

/**
 * A user's role in one project (docs/requirements.md, section 2). The order matters:
 * {@code VIEWER} is the lowest and {@code OWNER} the highest, so "at least Member" means
 * Member or Owner.
 */
public enum Role {
    VIEWER,
    MEMBER,
    OWNER;

    /** True if this role is the same as {@code minimum} or higher. */
    public boolean atLeast(Role minimum) {
        return compareTo(minimum) >= 0;
    }
}

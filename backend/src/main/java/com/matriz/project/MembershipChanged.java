package com.matriz.project;

/**
 * Published by the project module when a member is removed from a project or demoted to Viewer
 * (docs/architecture.md, section 5, port 2). Other modules listen to it to clean up their own
 * data; the project module never calls them.
 */
public record MembershipChanged(long projectId, long userId) {
}

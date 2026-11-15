package com.matriz.project;

/**
 * Body of {@code PUT /api/v1/projects/{projectId}/progress-override}: the Owner's manual progress,
 * a whole number from 0 to 100. Anything else, including a decimal or a text, is refused with {@code 400}.
 */
public record ProgressOverrideRequest(Integer percentage) {
}

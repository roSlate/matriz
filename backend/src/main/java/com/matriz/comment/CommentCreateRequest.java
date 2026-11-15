package com.matriz.comment;

/**
 * Body of {@code POST /api/v1/tasks/{taskId}/comments}. Only the text: the author and the time are
 * set by the server, and a request that contains anything else is refused with {@code 400}.
 */
public record CommentCreateRequest(String content) {
}

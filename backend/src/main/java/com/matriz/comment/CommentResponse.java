package com.matriz.comment;

import java.time.Instant;

/**
 * A comment as returned by the API. The author is shown by name only, never with an email: this is
 * the single exception to the rule that people see only the users they share a project with
 * (CC-01), and it keeps the comment readable after its author has left the project.
 */
public record CommentResponse(Long id, Long taskId, Author author, String content, Instant createdAt) {

    /** The name and identifier of the person who wrote a comment. */
    public record Author(Long id, String name) {
    }

    static CommentResponse of(Comment comment, Author author) {
        return new CommentResponse(comment.getId(), comment.getTaskId(), author, comment.getContent(),
                comment.getCreatedAt());
    }
}

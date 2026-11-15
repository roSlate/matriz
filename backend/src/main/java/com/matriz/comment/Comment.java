package com.matriz.comment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A comment on a task (table {@code comments}, see docs/database.md). The task and the author are
 * kept as plain identifiers: the comment module does not depend on the entities of other modules.
 * A comment is never edited or deleted on its own; it goes with its task.
 */
@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false, updatable = false)
    private Long taskId;

    @Column(name = "author_id", nullable = false, updatable = false)
    private Long authorId;

    @Column(nullable = false, updatable = false)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Comment() {
        // required by JPA
    }

    /** Creates a comment that has not been saved yet. The text arrives already trimmed. */
    public Comment(Long taskId, Long authorId, String content) {
        this.taskId = taskId;
        this.authorId = authorId;
        this.content = content;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

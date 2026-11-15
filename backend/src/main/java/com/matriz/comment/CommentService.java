package com.matriz.comment;

import com.matriz.common.FieldValidation;
import com.matriz.common.PageResponse;
import com.matriz.identity.UserDirectory;
import com.matriz.identity.UserSummary;
import com.matriz.project.ProjectAccess;
import com.matriz.project.Role;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business rules for comments (US-08). The caller's role in the task's project was found by the
 * task access step, which answered {@code 404} to outsiders. The order of checks then is: input
 * ({@code 400}), then the role ({@code 403}).
 */
@Service
public class CommentService {

    private static final int CONTENT_MIN = 1;
    private static final int CONTENT_MAX = 2000;

    private final CommentRepository comments;
    private final UserDirectory users;

    public CommentService(CommentRepository comments, UserDirectory users) {
        this.comments = comments;
        this.users = users;
    }

    /** One page of a task's comments with their authors' names. Any member of the project may read (AC-08.4). */
    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> list(long taskId, Pageable pageable) {
        Page<Comment> page = comments.findByTaskId(taskId, pageable);
        Map<Long, UserSummary> authors = users.summaries(
                page.getContent().stream().map(Comment::getAuthorId).distinct().toList());
        List<CommentResponse> items = page.getContent().stream()
                .map(comment -> CommentResponse.of(comment, author(authors.get(comment.getAuthorId()))))
                .toList();
        return PageResponse.of(page, items);
    }

    /**
     * Adds a comment by the caller, with the current time (AC-08.1 to AC-08.3, AC-08.6). Only Owners
     * and Members may comment. The text is stored trimmed.
     */
    @Transactional
    public CommentResponse add(long taskId, long authorId, Role role, CommentCreateRequest request) {
        FieldValidation.create()
                .requiredText("content", request.content(), CONTENT_MIN, CONTENT_MAX)
                .throwIfInvalid();
        ProjectAccess.requireAtLeast(role, Role.MEMBER);

        Comment saved = comments.save(new Comment(taskId, authorId, FieldValidation.trim(request.content())));
        UserSummary summary = users.summaries(List.of(authorId)).get(authorId);
        return CommentResponse.of(saved, author(summary));
    }

    /** Name and identifier only; the email stays behind (CC-01). */
    private static CommentResponse.Author author(UserSummary user) {
        return new CommentResponse.Author(user.id(), user.name());
    }
}

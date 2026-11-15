package com.matriz.comment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Database access for {@link Comment}. Used by the comment module only. */
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /** One page of a task's comments, in the order of the given {@code Pageable}. */
    Page<Comment> findByTaskId(Long taskId, Pageable pageable);
}

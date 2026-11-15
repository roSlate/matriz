package com.matriz.common;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * A page of results, as in the API contract. {@code page} starts at 0; {@code totalItems}
 * counts all matching items, not only those on this page.
 */
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    public static <S, T> PageResponse<T> of(Page<S> page, List<T> items) {
        return new PageResponse<>(items, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    /** Checks {@code page} and {@code size} as the contract requires (size 1 to 100) and builds the request. */
    public static Pageable pageable(int page, int size, Sort sort) {
        FieldValidation.create()
                .range("page", page, 0, Integer.MAX_VALUE)
                .range("size", size, 1, 100)
                .throwIfInvalid();
        return PageRequest.of(page, size, sort);
    }
}

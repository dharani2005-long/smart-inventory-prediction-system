package com.smartinventory.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Serialization-friendly wrapper around a Spring Data {@link Page}. */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    /** Map an entity {@code Page} into a DTO {@code PageResponse} via {@code mapper}. */
    public static <E, D> PageResponse<D> from(Page<E> page, Function<E, D> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}

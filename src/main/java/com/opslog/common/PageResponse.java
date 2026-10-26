package com.opslog.common;

import org.springframework.data.domain.Page;

import java.util.List;

/* stable JSON shape for paged results instead of serializing Spring's PageImpl */
public record PageResponse<T>(List<T> items, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}

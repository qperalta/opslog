package com.opslog.logging;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/* query-string filters for GET /api/history; every filter is optional */
public record HistorySearch(
        Long exerciseId,
        Long muscleGroupId,

        @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)
        LocalDate from,

        @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)
        LocalDate to,

        @Min(value=0, message="Page must be 0 or more")
        Integer page,

        @Min(value=1, message="Page size must be at least 1")
        @Max(value=100, message="Page size must be 100 or fewer")
        Integer size
) {
    public HistorySearch {
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
    }
}

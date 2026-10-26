package com.opslog.catalog;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/* query-string filters for GET /api/catalog/exercises; every filter is optional */
public record ExerciseSearch(
        @Size(max=80, message="Search text must be 80 characters or fewer")
        String q,

        Long muscleGroupId,
        Long movementPatternId,
        Long equipmentId,
        String kind,

        @Min(value=0, message="Page must be 0 or more")
        Integer page,

        @Min(value=1, message="Page size must be at least 1")
        @Max(value=100, message="Page size must be 100 or fewer")
        Integer size
) {
    public ExerciseSearch {
        q = q == null
                ? null
                : q.strip();
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
    }
}

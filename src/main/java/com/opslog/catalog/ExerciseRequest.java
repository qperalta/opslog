package com.opslog.catalog;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

/* body for POST and PUT /api/catalog/exercises; the kind-specific fields are checked by the entity setters */
public record ExerciseRequest(
        @NotBlank(message="Kind is required")
        String kind,

        @NotBlank(message="Name is required")
        @Size(max=80, message="Name must be 80 characters or fewer")
        String name,

        @NotNull(message="Movement pattern is required")
        Long movementPatternId,

        @NotNull(message="Primary muscle group is required")
        Long primaryMuscleId,

        @Min(value=1, message="Minimum reps must be at least 1")
        @Max(value=50, message="Minimum reps must be 50 or fewer")
        Integer repMin,

        @Min(value=1, message="Maximum reps must be at least 1")
        @Max(value=50, message="Maximum reps must be 50 or fewer")
        Integer repMax,

        @Min(value=1, message="Target time must be at least 1 second")
        @Max(value=600, message="Target time must be 600 seconds or less")
        Integer targetSeconds,

        @Min(value=1, message="Duration must be at least 1 minute")
        @Max(value=60, message="Duration must be 60 minutes or less")
        Integer durationMinutes,

        @Size(max=10, message="An exercise can list at most 10 equipment items")
        Set<@NotNull Long> equipmentIds
) {
    public ExerciseRequest {
        kind = kind == null
                ? null
                : kind.strip().toUpperCase();
        name = name == null
                ? null
                : name.strip();
        equipmentIds = equipmentIds == null ? Set.of() : Set.copyOf(equipmentIds);
    }
}

package com.opslog.session;

import com.opslog.catalog.ExerciseRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/* body for POST /api/sessions/{date}/exercises: exerciseId for one from the list, or newExercise to create the member's own */
public record InsertExerciseRequest(
        @NotNull(message="Position is required")
        @Min(value=1, message="Position must be at least 1")
        Integer position,

        Long exerciseId,

        @Valid
        ExerciseRequest newExercise,

        @NotNull(message="Sets are required")
        @Min(value=1, message="Sets must be at least 1")
        @Max(value=10, message="Sets must be 10 or fewer")
        Integer sets,

        @DecimalMin(value="0", message="Target load cannot be negative")
        @DecimalMax(value="2000", message="Target load must be 2000 lb or less")
        BigDecimal targetLoadLb
) {}

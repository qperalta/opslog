package com.opslog.session;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/* body for PUT /api/exercise-timings/{exerciseId}; a null clears that time back to the default */
public record ExerciseTimingRequest(
        @Min(value=1, message="Timer must be at least 0:01")
        @Max(value=600, message="Timer must be 10:00 or less")
        Integer holdSeconds,

        @Min(value=0, message="Rest cannot be negative")
        @Max(value=900, message="Rest must be 15:00 or less")
        Integer restSeconds
) {}

package com.opslog.logging;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

/* reps for strength, seconds for timed and cardio; the entity checks which */
public record LoggedSetRequest(
        @Min(value=0, message="Reps must be between 0 and 100")
        @Max(value=100, message="Reps must be between 0 and 100")
        Integer reps,

        @Min(value=1, message="Seconds must be between 1 and 7200")
        @Max(value=7200, message="Seconds must be between 1 and 7200")
        Integer durationSeconds,

        @DecimalMin(value="0", message="Load cannot be negative")
        @Digits(integer=4, fraction=2, message="Load must be under 10000 lb with at most 2 decimals")
        BigDecimal loadLb,

        Boolean clean
) {
    /* technique is clean unless the lifter says otherwise */
    public LoggedSetRequest {
        clean = clean == null || clean;
    }
}

package com.opslog.checkin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CheckInRequest(
        @NotNull(message="Sleep quality is required")
        @Min(value=1, message="Sleep quality must be between 1 and 5")
        @Max(value=5, message="Sleep quality must be between 1 and 5")
        Integer sleepQuality,

        @NotNull(message="Soreness is required")
        @Min(value=1, message="Soreness must be between 1 and 5")
        @Max(value=5, message="Soreness must be between 1 and 5")
        Integer soreness,

        @NotNull(message="Location is required")
        Long locationId
) {}

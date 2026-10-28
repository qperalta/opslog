package com.opslog.plan;

import jakarta.validation.constraints.NotNull;

/* workoutId only for TRAINING; null lets the rules engine pick */
public record PlanDayRequest(
        @NotNull(message="Mode is required")
        PlanMode mode,

        Long workoutId,

        Long locationId,

        Boolean locked
) {

    /* Jackson 3 rejects a missing primitive, so default it here */
    public PlanDayRequest {
        if(locked == null) locked = false;
    }
}

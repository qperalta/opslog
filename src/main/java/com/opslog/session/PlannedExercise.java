package com.opslog.session;

import com.opslog.catalog.Exercise;
import com.opslog.common.Require;

/* a template exercise as written, before tier and location */
public record PlannedExercise(Exercise exercise, int sets, boolean mainLift) {

    public PlannedExercise {
        Require.that(exercise != null, "Exercise is required");
        Require.that(sets > 0, "Planned sets must be greater than zero");
    }
}

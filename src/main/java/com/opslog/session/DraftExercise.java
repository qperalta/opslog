package com.opslog.session;

import com.opslog.catalog.Exercise;
import com.opslog.common.Require;

import java.math.BigDecimal;

/* substitutedFor null = kept as planned; reason null = no change */
public record DraftExercise(Exercise exercise, Exercise substitutedFor, int sets, BigDecimal targetLoadLb,
                            boolean mainLift, String reason) {

    public DraftExercise {
        Require.that(exercise != null, "Exercise is required");
        Require.that(sets > 0, "Sets must be greater than zero");
    }
}

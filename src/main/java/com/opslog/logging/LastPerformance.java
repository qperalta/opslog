package com.opslog.logging;

import com.opslog.catalog.Exercise;
import com.opslog.common.Require;

import java.util.List;

/* the most recent logged session of one exercise */
public record LastPerformance(Exercise exercise, int plannedSets, List<SetResult> sets) {

    public LastPerformance {
        Require.that(exercise != null, "Exercise is required");
        Require.that(plannedSets > 0, "Planned sets must be greater than zero");
        sets = sets == null ? List.of() : List.copyOf(sets);
    }
}

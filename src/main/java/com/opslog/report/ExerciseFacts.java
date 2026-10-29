package com.opslog.report;

import com.opslog.logging.SetResult;

import java.util.List;

/* one session exercise; direction is null when the pattern is neither push nor pull */
public record ExerciseFacts(long muscleId, Direction direction, int plannedSets, List<SetResult> sets) {

    public ExerciseFacts {
        sets = sets == null ? List.of() : List.copyOf(sets);
    }
}

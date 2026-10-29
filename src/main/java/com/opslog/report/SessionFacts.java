package com.opslog.report;

import java.time.LocalDate;
import java.util.List;

/* one stored session and everything the reports count from it */
public record SessionFacts(LocalDate date, String workout, String tier, String location, List<ExerciseFacts> exercises) {

    public SessionFacts {
        exercises = exercises == null ? List.of() : List.copyOf(exercises);
    }
}

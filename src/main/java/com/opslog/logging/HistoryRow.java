package com.opslog.logging;

import com.opslog.catalog.Exercise;
import com.opslog.session.SessionExercise;
import com.opslog.session.SessionExerciseResponse;
import com.opslog.session.TrainingSession;

import java.math.BigDecimal;
import java.time.LocalDate;

/* one logged set with the session it belongs to */
public record HistoryRow(LocalDate date, String workout, String location, Long exerciseId, String exercise, String kind,
                         String primaryMuscle, int setNumber, Integer reps, Integer durationSeconds, BigDecimal loadLb,
                         boolean clean) {

    static HistoryRow from(LoggedSet set) {
        SessionExercise item = set.getSessionExercise();
        TrainingSession session = item.getSession();
        Exercise exercise = item.getExercise();
        return new HistoryRow(session.getDate(),
                session.getWorkout() == null ? null : session.getWorkout().getName(),
                session.getLocation() == null ? null : session.getLocation().getName(),
                exercise.getId(), exercise.getName(), SessionExerciseResponse.kindOf(exercise),
                exercise.getPrimaryMuscle().getName(), set.getSetNumber(), set.getReps(), set.getDurationSeconds(),
                set.getLoadLb(), set.isClean());
    }
}

package com.opslog.report;

import com.opslog.logging.SetResult;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/* one flat query (session x exercise x set), grouped back into sessions; newest day first */
@Repository
class ReportFacts {

    private static final String SQL = """
            SELECT s.id AS session_id, s.session_date, td.name AS workout, t.name AS tier, l.name AS location,
                   se.id AS session_exercise_id, se.sets AS planned_sets, e.primary_muscle_id AS muscle_id,
                   mp.direction, ls.reps, ls.load_lb, ls.clean
            FROM training_sessions s
            JOIN training_tiers t ON t.id = s.tier_id
            LEFT JOIN template_days td ON td.id = s.template_day_id
            LEFT JOIN locations l ON l.id = s.location_id
            LEFT JOIN session_exercises se ON se.session_id = s.id
            LEFT JOIN exercises e ON e.id = se.exercise_id
            LEFT JOIN movement_patterns mp ON mp.id = e.movement_pattern_id
            LEFT JOIN logged_sets ls ON ls.session_exercise_id = se.id
            WHERE s.user_id = :userId AND s.session_date BETWEEN :from AND :to
            ORDER BY s.session_date DESC, se.position, ls.set_number""";

    private final JdbcClient jdbc;

    ReportFacts(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    List<SessionFacts> sessions(long userId, LocalDate from, LocalDate to) {
        List<FactRow> rows = jdbc.sql(SQL)
                .param("userId", userId)
                .param("from", from)
                .param("to", to)
                .query(FactRow.class)
                .list();

        Map<Long, SessionDraft> sessions = new LinkedHashMap<>();
        for(FactRow row : rows) {
            SessionDraft session = sessions.computeIfAbsent(row.sessionId(), id -> new SessionDraft(row));
            if(row.sessionExerciseId() == null) continue;
            ExerciseDraft exercise = session.exercises.computeIfAbsent(row.sessionExerciseId(), id -> new ExerciseDraft(row));
            if(row.clean() != null) exercise.sets.add(new SetResult(row.reps(), row.loadLb(), row.clean()));
        }
        return sessions.values().stream().map(SessionDraft::toFacts).toList();
    }

    /* columns map by name: session_date -> sessionDate */
    record FactRow(long sessionId, LocalDate sessionDate, String workout, String tier, String location,
                   Long sessionExerciseId, Integer plannedSets, Long muscleId, String direction,
                   Integer reps, BigDecimal loadLb, Boolean clean) {}

    private static final class SessionDraft {
        private final FactRow first;
        private final Map<Long, ExerciseDraft> exercises = new LinkedHashMap<>();

        SessionDraft(FactRow first) { this.first = first; }

        SessionFacts toFacts() {
            // a session the agent wrote outside the template has no workout name
            String workout = first.workout() == null ? "Custom" : first.workout();
            return new SessionFacts(first.sessionDate(), workout, first.tier(), first.location(),
                    exercises.values().stream().map(ExerciseDraft::toFacts).toList());
        }
    }

    private static final class ExerciseDraft {
        private final FactRow first;
        private final List<SetResult> sets = new ArrayList<>();

        ExerciseDraft(FactRow first) { this.first = first; }

        ExerciseFacts toFacts() {
            Direction direction = first.direction() == null ? null : Direction.valueOf(first.direction());
            return new ExerciseFacts(first.muscleId(), direction, first.plannedSets(), sets);
        }
    }
}

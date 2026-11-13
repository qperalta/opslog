package com.opslog.session;

import com.opslog.logging.LoggedSetResponse;
import com.opslog.catalog.LookupResponse;
import com.opslog.plan.Actor;
import com.opslog.plan.TemplateDay;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/* tierKind lets the UI color the tier without a second lookup */
public record SessionResponse(LocalDate date, SessionStatus status, LookupResponse workout, LookupResponse tier, String tierKind,
                              LookupResponse location, Actor source, List<String> notes,
                              List<SessionExerciseResponse> exercises, Instant createdAt) {

    static SessionResponse rest(LocalDate date) {
        return new SessionResponse(date, SessionStatus.REST, null, null, null, null, null, List.of(), List.of(), null);
    }

    static SessionResponse needsCheckIn(LocalDate date, TemplateDay workout) {
        return new SessionResponse(date, SessionStatus.NEEDS_CHECKIN, lookup(workout), null, null, null, null,
                List.of(), List.of(), null);
    }

    static SessionResponse from(TrainingSession session, Map<Long, LoggedSetResponse> lastSets, Map<Long, ExerciseTiming> timings) {
        List<String> notes = session.getRationale() == null || session.getRationale().isBlank()
                ? List.of()
                : List.of(session.getRationale().split("\n"));
        LookupResponse location = session.getLocation() == null
                ? null
                : new LookupResponse(session.getLocation().getId(), session.getLocation().getName());
        return new SessionResponse(session.getDate(), SessionStatus.READY, lookup(session.getWorkout()),
                new LookupResponse(session.getTier().getId(), session.getTier().getName()),
                SessionExerciseResponse.kindOf(session.getTier()), location, session.getSource(), notes,
                session.getExercises().stream().map(item -> SessionExerciseResponse.from(item, lastSets, timings)).toList(), session.getCreatedAt());
    }

    private static LookupResponse lookup(TemplateDay workout) {
        return workout == null ? null : new LookupResponse(workout.getId(), workout.getName());
    }
}

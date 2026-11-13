package com.opslog.session;

import com.opslog.catalog.Exercise;
import com.opslog.catalog.LookupResponse;
import com.opslog.logging.LoggedSetResponse;
import jakarta.persistence.DiscriminatorValue;
import org.hibernate.Hibernate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record SessionExerciseResponse(long id, int position, Long exerciseId, String name, String kind, String target, int sets,
                                      BigDecimal targetLoadLb, boolean mainLift, LookupResponse substitutedFor, String reason, boolean skipped,
                                      boolean adHoc, List<LoggedSetResponse> logged, LoggedSetResponse lastTime) {

    static SessionExerciseResponse from(SessionExercise item) { return from(item, Map.of()); }

    /* lastTime = the last set of this exercise in an earlier session, or null */
    static SessionExerciseResponse from(SessionExercise item, Map<Long, LoggedSetResponse> lastSets) {
        Exercise exercise = item.getExercise();
        LookupResponse substitutedFor = item.getSubstitutedFor() == null
                ? null
                : new LookupResponse(item.getSubstitutedFor().getId(), item.getSubstitutedFor().getName());
        return new SessionExerciseResponse(item.getId(), item.getPosition(), exercise.getId(), exercise.getName(), kindOf(exercise),
                exercise.describeTarget(), item.getSets(), item.getTargetLoadLb(), item.isMainLift(), substitutedFor, item.getReason(), item.isSkipped(),
                item.isAdHoc(), item.getLoggedSets().stream().map(LoggedSetResponse::from).toList(), lastSets.get(exercise.getId()));
    }

    /* getClass() unwraps a lazy proxy */
    public static String kindOf(Object entity) {
        return Hibernate.getClass(entity).getAnnotation(DiscriminatorValue.class).value();
    }
}

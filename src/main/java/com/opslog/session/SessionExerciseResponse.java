package com.opslog.session;

import com.opslog.catalog.Exercise;
import com.opslog.catalog.LookupResponse;
import com.opslog.logging.LoggedSetResponse;
import jakarta.persistence.DiscriminatorValue;
import org.hibernate.Hibernate;

import java.math.BigDecimal;
import java.util.List;

public record SessionExerciseResponse(int position, Long exerciseId, String name, String kind, String target, int sets,
                                      BigDecimal targetLoadLb, boolean mainLift, LookupResponse substitutedFor, String reason, boolean skipped,
                                      List<LoggedSetResponse> logged) {

    static SessionExerciseResponse from(SessionExercise item) {
        Exercise exercise = item.getExercise();
        LookupResponse substitutedFor = item.getSubstitutedFor() == null
                ? null
                : new LookupResponse(item.getSubstitutedFor().getId(), item.getSubstitutedFor().getName());
        return new SessionExerciseResponse(item.getPosition(), exercise.getId(), exercise.getName(), kindOf(exercise),
                exercise.describeTarget(), item.getSets(), item.getTargetLoadLb(), item.isMainLift(), substitutedFor, item.getReason(), item.isSkipped(),
                item.getLoggedSets().stream().map(LoggedSetResponse::from).toList());
    }

    /* getClass() unwraps a lazy proxy */
    public static String kindOf(Object entity) {
        return Hibernate.getClass(entity).getAnnotation(DiscriminatorValue.class).value();
    }
}

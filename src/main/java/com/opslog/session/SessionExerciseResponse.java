package com.opslog.session;

import com.opslog.catalog.Exercise;
import com.opslog.catalog.LookupResponse;
import jakarta.persistence.DiscriminatorValue;
import org.hibernate.Hibernate;

import java.math.BigDecimal;

public record SessionExerciseResponse(int position, Long exerciseId, String name, String kind, String target, int sets,
                                      BigDecimal targetLoadLb, boolean mainLift, LookupResponse substitutedFor, String reason) {

    static SessionExerciseResponse from(SessionExercise item) {
        Exercise exercise = item.getExercise();
        LookupResponse substitutedFor = item.getSubstitutedFor() == null
                ? null
                : new LookupResponse(item.getSubstitutedFor().getId(), item.getSubstitutedFor().getName());
        return new SessionExerciseResponse(item.getPosition(), exercise.getId(), exercise.getName(), kindOf(exercise),
                exercise.describeTarget(), item.getSets(), item.getTargetLoadLb(), item.isMainLift(), substitutedFor, item.getReason());
    }

    /* getClass() unwraps a lazy proxy */
    static String kindOf(Object entity) {
        return Hibernate.getClass(entity).getAnnotation(DiscriminatorValue.class).value();
    }
}

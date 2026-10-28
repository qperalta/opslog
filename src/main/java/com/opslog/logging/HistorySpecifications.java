package com.opslog.logging;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/* composable WHERE clauses for history search; the criteria API binds every value as a parameter */
final class HistorySpecifications {

    private HistorySpecifications() {}

    static Specification<LoggedSet> ownedBy(long userId) {
        return (root, query, cb) -> cb.equal(root.get("sessionExercise").get("session").get("userId"), userId);
    }

    static Specification<LoggedSet> ofExercise(Long exerciseId) {
        if(exerciseId == null) return Specification.unrestricted();
        return (root, query, cb) -> cb.equal(root.get("sessionExercise").get("exercise").get("id"), exerciseId);
    }

    static Specification<LoggedSet> forMuscle(Long muscleGroupId) {
        if(muscleGroupId == null) return Specification.unrestricted();
        return (root, query, cb) -> cb.equal(root.get("sessionExercise").get("exercise").get("primaryMuscle").get("id"), muscleGroupId);
    }

    static Specification<LoggedSet> onOrAfter(LocalDate from) {
        if(from == null) return Specification.unrestricted();
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("sessionExercise").get("session").get("date"), from);
    }

    static Specification<LoggedSet> onOrBefore(LocalDate to) {
        if(to == null) return Specification.unrestricted();
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("sessionExercise").get("session").get("date"), to);
    }
}

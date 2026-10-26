package com.opslog.catalog;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.Map;

/* Composable WHERE clauses for exercise search; the criteria API binds every value as a parameter */
final class ExerciseSpecifications {

    private static final Map<String, Class<? extends Exercise>> KINDS = Map.of(
            "STRENGTH", StrengthExercise.class,
            "TIMED", TimedExercise.class,
            "CARDIO", CardioFinisher.class);

    private ExerciseSpecifications() {}

    /* shared catalog rows plus the user's own custom exercises */
    static Specification<Exercise> visibleTo(long userId) {
        return (root, query, cb) -> cb.or(
                cb.isNull(root.get("ownerId")),
                cb.equal(root.get("ownerId"), userId));
    }

    static Specification<Exercise> nameContains(String text) {
        if(text == null || text.isBlank()) return Specification.unrestricted();
        String pattern = "%" + escapeLike(text.strip().toLowerCase()) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern, '\\');
    }

    static Specification<Exercise> hasPrimaryMuscle(Long muscleGroupId) {
        if(muscleGroupId == null) return Specification.unrestricted();
        return (root, query, cb) -> cb.equal(root.get("primaryMuscle").get("id"), muscleGroupId);
    }

    static Specification<Exercise> hasMovementPattern(Long movementPatternId) {
        if(movementPatternId == null) return Specification.unrestricted();
        return (root, query, cb) -> cb.equal(root.get("movementPattern").get("id"), movementPatternId);
    }

    /* EXISTS subquery instead of a join, so one exercise never appears twice in a page */
    static Specification<Exercise> usesEquipment(Long equipmentId) {
        if(equipmentId == null) return Specification.unrestricted();
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<Exercise> inner = sub.from(Exercise.class);
            Join<Exercise, Equipment> item = inner.join("equipment");
            sub.select(inner.get("id")).where(
                    cb.equal(inner.get("id"), root.get("id")),
                    cb.equal(item.get("id"), equipmentId));
            return cb.exists(sub);
        };
    }

    static Specification<Exercise> ofKind(String kind) {
        if(kind == null || kind.isBlank()) return Specification.unrestricted();
        Class<? extends Exercise> type = KINDS.get(kind.strip().toUpperCase());
        if(type == null) throw new IllegalArgumentException("Unknown exercise kind: " + kind.strip());
        return (root, query, cb) -> cb.equal(root.type(), type);
    }

    /* treat % and _ typed by the user as literal characters */
    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}

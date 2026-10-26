package com.opslog.catalog;

import java.util.Comparator;
import java.util.List;

public record ExerciseResponse(
        Long id,
        String name,
        String kind,
        LookupResponse movementPattern,
        LookupResponse primaryMuscle,
        String target,
        Integer repMin,
        Integer repMax,
        Integer targetSeconds,
        Integer durationMinutes,
        boolean custom,
        List<LookupResponse> equipment
) {

    static ExerciseResponse from(Exercise exercise) {
        Integer repMin = null, repMax = null, targetSeconds = null, durationMinutes = null;
        if(exercise instanceof StrengthExercise strength) {
            repMin = strength.getRepMin();
            repMax = strength.getRepMax();
        } else if(exercise instanceof TimedExercise timed) {
            targetSeconds = timed.getTargetSeconds();
        } else if(exercise instanceof CardioFinisher cardio) {
            durationMinutes = cardio.getDurationMinutes();
        }

        List<LookupResponse> equipment = exercise.getEquipment().stream()
                .map(LookupResponse::from)
                .sorted(Comparator.comparing(LookupResponse::name))
                .toList();

        return new ExerciseResponse(exercise.getId(), exercise.getName(), EquipmentResponse.kindOf(exercise),
                LookupResponse.from(exercise.getMovementPattern()), LookupResponse.from(exercise.getPrimaryMuscle()),
                exercise.describeTarget(), repMin, repMax, targetSeconds, durationMinutes,
                exercise.isCustom(), equipment);
    }
}

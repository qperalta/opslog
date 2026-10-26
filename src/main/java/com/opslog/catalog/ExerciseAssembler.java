package com.opslog.catalog;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/* turns an ExerciseRequest into entity state: resolves lookup ids and picks the subclass for the kind */
@Component
class ExerciseAssembler {

    private final MovementPatternRepository movementPatterns;
    private final MuscleGroupRepository muscleGroups;
    private final EquipmentRepository equipment;

    ExerciseAssembler(MovementPatternRepository movementPatterns, MuscleGroupRepository muscleGroups,
                      EquipmentRepository equipment) {
        this.movementPatterns = movementPatterns;
        this.muscleGroups = muscleGroups;
        this.equipment = equipment;
    }

    Exercise newExercise(long ownerId, ExerciseRequest request) {
        MovementPattern pattern = movementPattern(request.movementPatternId());
        MuscleGroup muscle = muscleGroup(request.primaryMuscleId());
        Exercise exercise = switch(request.kind()) {
            case "STRENGTH" -> new StrengthExercise(request.name(), pattern, muscle, ownerId,
                    required(request.repMin(), "Minimum reps"), required(request.repMax(), "Maximum reps"));
            case "TIMED" -> new TimedExercise(request.name(), pattern, muscle, ownerId,
                    required(request.targetSeconds(), "Target time"));
            case "CARDIO" -> new CardioFinisher(request.name(), pattern, muscle, ownerId,
                    required(request.durationMinutes(), "Duration"));
            default -> throw new IllegalArgumentException("Unknown exercise kind: " + request.kind());
        };
        equipment(request.equipmentIds()).forEach(exercise::addEquipment);
        return exercise;
    }

    /* the kind is fixed after creation, so only the matching subclass fields are applied */
    void applyTo(Exercise exercise, ExerciseRequest request) {
        exercise.setName(request.name());
        exercise.setMovementPattern(movementPattern(request.movementPatternId()));
        exercise.setPrimaryMuscle(muscleGroup(request.primaryMuscleId()));
        if(exercise instanceof StrengthExercise strength) {
            strength.setRepRange(required(request.repMin(), "Minimum reps"), required(request.repMax(), "Maximum reps"));
        } else if(exercise instanceof TimedExercise timed) {
            timed.setTargetSeconds(required(request.targetSeconds(), "Target time"));
        } else if(exercise instanceof CardioFinisher cardio) {
            cardio.setDurationMinutes(required(request.durationMinutes(), "Duration"));
        }
        List<Equipment> wanted = equipment(request.equipmentIds());
        Set.copyOf(exercise.getEquipment()).stream()
                .filter(item -> !wanted.contains(item))
                .forEach(exercise::removeEquipment);
        wanted.forEach(exercise::addEquipment);
    }

    private MovementPattern movementPattern(Long id) {
        return movementPatterns.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown movement pattern: " + id));
    }

    private MuscleGroup muscleGroup(Long id) {
        return muscleGroups.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown muscle group: " + id));
    }

    private List<Equipment> equipment(Set<Long> ids) {
        List<Equipment> found = equipment.findAllById(ids);
        if(found.size() != ids.size()) {
            Set<Long> missing = new HashSet<>(ids);
            found.forEach(item -> missing.remove(item.getId()));
            throw new IllegalArgumentException("Unknown equipment: " + missing);
        }
        return found;
    }

    private static int required(Integer value, String field) {
        if(value == null) throw new IllegalArgumentException(field + " is required for this kind of exercise");
        return value;
    }
}

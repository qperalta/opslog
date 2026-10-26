package com.opslog.catalog;

import com.opslog.common.Require;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CustomExerciseService {

    private static final long NO_EXCLUSION = 0L; // start at 1, 0 will mean no row to exclude

    private final ExerciseRepository exercises;
    private final ExerciseAssembler assembler;

    // removed public since it gives warning of ExerciseAssembler exposed outside of scope.
    // It's private package anyway.
    CustomExerciseService(ExerciseRepository exercises, ExerciseAssembler assembler) {
        this.exercises = exercises;
        this.assembler = assembler;
    }

    @Transactional(readOnly = true)
    public ExerciseResponse get(long userId, long id) { return ExerciseResponse.from(findVisible(userId, id)); }

    public ExerciseResponse create(long userId, ExerciseRequest request) {
        requireUniqueName(userId, request.name(), NO_EXCLUSION);
        Exercise exercise = assembler.newExercise(userId, request);
        return ExerciseResponse.from(exercises.saveAndFlush(exercise));
    }

    public ExerciseResponse update(long userId, long id, ExerciseRequest request) {
        Exercise exercise = findOwned(userId, id);
        Require.that(EquipmentResponse.kindOf(exercise).equals(request.kind()), "Exercise kind cannot be changed; create a new exercise instead");
        requireUniqueName(userId, request.name(), id);
        assembler.applyTo(exercise, request);
        return ExerciseResponse.from(exercises.saveAndFlush(exercise));
    }

    public void delete(long userId, long id) { exercises.delete(findOwned(userId, id)); }

    private Exercise findVisible(long userId, long id) {
        Exercise exercise = exercises.findById(id).orElseThrow(() -> new ExerciseNotFoundException(id));
        if(!exercise.isVisibleTo(userId)) throw new ExerciseNotFoundException(id);
        return exercise;
    }

    private Exercise findOwned(long userId, long id) {
        Exercise exercise = findVisible(userId, id);
        if(!exercise.isCustom()) throw new ExerciseReadOnlyException();
        return exercise;
    }

    private void requireUniqueName(long userId, String name, long excludeId) {
        if(exercises.isNameTaken(name, userId, excludeId)) throw new DuplicateExerciseNameException(name);
    }
}

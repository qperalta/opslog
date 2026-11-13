package com.opslog.session;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExerciseTimingRepository extends JpaRepository<ExerciseTiming, Long> {
    Optional<ExerciseTiming> findByUserIdAndExerciseId(long userId, long exerciseId);

    List<ExerciseTiming> findByUserId(long userId);
}

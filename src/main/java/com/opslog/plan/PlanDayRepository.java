package com.opslog.plan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PlanDayRepository extends JpaRepository<PlanDay, Long> {

    Optional<PlanDay> findByUserIdAndDate(long userId, LocalDate date);

    List<PlanDay> findByUserIdAndDateBetweenOrderByDate(long userId, LocalDate from, LocalDate to);

    List<PlanDay> findByUserIdAndDateGreaterThanEqualOrderByDate(long userId, LocalDate from);

    /* where the rotation picks up */
    Optional<PlanDay> findFirstByUserIdAndDateBeforeAndWorkoutIsNotNullOrderByDateDesc(long userId, LocalDate date);
}

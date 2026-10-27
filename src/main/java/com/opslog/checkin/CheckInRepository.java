package com.opslog.checkin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CheckInRepository extends JpaRepository<CheckIn, Long> {

    Optional<CheckIn> findByUserIdAndDate(long userId, LocalDate date);

    List<CheckIn> findByUserIdAndDateBetweenOrderByDateDesc(long userId, LocalDate from, LocalDate to);
}

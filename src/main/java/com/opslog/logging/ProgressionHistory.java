package com.opslog.logging;

import com.opslog.session.SessionExercise;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.time.LocalDate;
import java.util.List;

/* newest first; recovery days run lighter loads, so they do not count */
interface ProgressionHistory extends Repository<SessionExercise, Long> {

    @Query("""
            SELECT se FROM SessionExercise se JOIN se.session s JOIN s.tier t
            WHERE s.userId = :userId AND s.date >= :since AND s.date < :date
              AND TYPE(t) <> RecoveryTier
              AND EXISTS (SELECT 1 FROM LoggedSet ls WHERE ls.sessionExercise = se)
            ORDER BY s.date DESC""")
    List<SessionExercise> findLoggedBetween(long userId, LocalDate since, LocalDate date);
}

package com.opslog.logging;

import com.opslog.session.SessionExercise;
import com.opslog.session.TrainingSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/* the session lookup is by user and date, so another user's session is never found */
@Service
@Transactional
public class LoggingService {

    private final TrainingSessionRepository sessions;

    LoggingService(TrainingSessionRepository sessions) {
        this.sessions = sessions;
    }

    public LoggedSetResponse log(long userId, LocalDate date, int position, LoggedSetRequest request) {
        SessionExercise item = exercise(userId, date, position);
        LoggedSet set = item.logSet(request.reps(), request.durationSeconds(), request.loadLb(), request.clean());
        sessions.flush();
        return LoggedSetResponse.from(set);
    }

    public LoggedSetResponse update(long userId, LocalDate date, int position, int setNumber, LoggedSetRequest request) {
        LoggedSet set = loggedSet(exercise(userId, date, position), setNumber);
        set.record(request.reps(), request.durationSeconds(), request.loadLb(), request.clean());
        return LoggedSetResponse.from(set);
    }

    public void delete(long userId, LocalDate date, int position, int setNumber) {
        SessionExercise item = exercise(userId, date, position);
        item.removeLoggedSet(loggedSet(item, setNumber));
    }

    private SessionExercise exercise(long userId, LocalDate date, int position) {
        return sessions.findByUserIdAndDate(userId, date)
                .flatMap(session -> session.findExercise(position))
                .orElseThrow(() -> new SessionExerciseNotFoundException(date, position));
    }

    private static LoggedSet loggedSet(SessionExercise item, int setNumber) {
        return item.findLoggedSet(setNumber).orElseThrow(() -> new LoggedSetNotFoundException(setNumber));
    }
}

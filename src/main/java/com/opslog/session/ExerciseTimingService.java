package com.opslog.session;

import com.opslog.catalog.Exercise;
import com.opslog.catalog.ExerciseNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class ExerciseTimingService {

    private final ExerciseTimingRepository timings;
    private final SessionCatalog catalog;

    ExerciseTimingService(ExerciseTimingRepository timings, SessionCatalog catalog) {
        this.timings = timings;
        this.catalog = catalog;
    }

    /* both null removes the row, so the defaults apply again */
    public ExerciseTimingResponse set(long userId, long exerciseId, Integer holdSeconds, Integer restSeconds) {
        Exercise exercise = catalog.findVisible(exerciseId, userId).orElseThrow(() -> new ExerciseNotFoundException(exerciseId));
        boolean timed = "TIMED".equals(SessionExerciseResponse.kindOf(exercise));
        ExerciseTiming timing = timings.findByUserIdAndExerciseId(userId, exerciseId)
                .orElseGet(() -> new ExerciseTiming(userId, exerciseId));
        timing.setTimes(holdSeconds, restSeconds, timed);
        if(timing.isEmpty()) {
            if(timing.getId() != null) timings.delete(timing);
        } else {
            timings.save(timing);
        }
        return new ExerciseTimingResponse(exerciseId, timing.getHoldSeconds(), timing.getRestSeconds());
    }

    @Transactional(readOnly = true)
    public Map<Long, ExerciseTiming> forMember(long userId) {
        return timings.findByUserId(userId).stream()
                .collect(Collectors.toMap(ExerciseTiming::getExerciseId, Function.identity()));
    }
}

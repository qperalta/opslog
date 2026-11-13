package com.opslog.logging;

import com.opslog.catalog.Exercise;
import com.opslog.session.SessionExercise;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class ProgressionService {

    /* older logs no longer say much about today's strength */
    private static final int HISTORY_DAYS = 90;

    private final ProgressionHistory history;
    private final ProgressionRule rule;

    ProgressionService(ProgressionHistory history, ProgressionRule rule) {
        this.history = history;
        this.rule = rule;
    }

    /* keyed by every exercise in the history, because the substitute is not chosen yet */
    public Map<Long, BigDecimal> plannedLoads(long userId, LocalDate date) {
        Map<Long, BigDecimal> loads = new HashMap<>();
        Set<Long> seen = new HashSet<>();
        for(SessionExercise item : history.findLoggedBetween(userId, date.minusDays(HISTORY_DAYS), date)) {
            Exercise exercise = Hibernate.unproxy(item.getExercise(), Exercise.class);
            if(!seen.add(exercise.getId())) continue;
            List<SetResult> sets = item.getLoggedSets().stream().map(SetResult::from).toList();
            BigDecimal next = rule.nextLoad(new LastPerformance(exercise, item.getSets(), sets));
            if(next != null) loads.put(exercise.getId(), next);
        }
        return loads;
    }

    /* the last set logged for each exercise in its most recent earlier session; blank fields on Today fall back to it */
    @Transactional(readOnly = true)
    public Map<Long, LoggedSetResponse> lastSets(long userId, LocalDate date) {
        Map<Long, LoggedSetResponse> last = new HashMap<>();
        for(SessionExercise item : history.findLoggedBetween(userId, date.minusDays(HISTORY_DAYS), date)) {
            Long exerciseId = item.getExercise().getId();
            if(last.containsKey(exerciseId)) continue;
            item.getLoggedSets().stream()
                    .max(Comparator.comparingInt(LoggedSet::getSetNumber))
                    .ifPresent(set -> last.put(exerciseId, LoggedSetResponse.from(set)));
        }
        return last;
    }
}

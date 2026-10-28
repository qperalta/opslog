package com.opslog.logging;

import com.opslog.common.PageResponse;
import com.opslog.common.Require;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class HistoryService {

    /* newest day first, then the order the sets were done */
    private static final Sort ORDER = Sort.by(Sort.Order.desc("sessionExercise.session.date"),
            Sort.Order.asc("sessionExercise.position"), Sort.Order.asc("setNumber"));

    private final LoggedSetRepository loggedSets;

    HistoryService(LoggedSetRepository loggedSets) {
        this.loggedSets = loggedSets;
    }

    public PageResponse<HistoryRow> search(long userId, HistorySearch search) {
        Require.that(search.from() == null || search.to() == null || !search.from().isAfter(search.to()),
                "The start date must be on or before the end date");
        Specification<LoggedSet> spec = Specification.allOf(
                HistorySpecifications.ownedBy(userId),
                HistorySpecifications.ofExercise(search.exerciseId()),
                HistorySpecifications.forMuscle(search.muscleGroupId()),
                HistorySpecifications.onOrAfter(search.from()),
                HistorySpecifications.onOrBefore(search.to()));
        Pageable pageable = PageRequest.of(search.page(), search.size(), ORDER);
        return PageResponse.from(loggedSets.findAll(spec, pageable).map(HistoryRow::from));
    }
}

package com.opslog.report;

import com.opslog.catalog.CatalogService;
import com.opslog.common.Require;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

/* the MCP get_training_report tool reuses this, so the agent sees the same numbers */
@Service
@Transactional(readOnly=true)
public class ReportService {

    static final int DEFAULT_DAYS = 28;
    static final int MAX_DAYS = 366;

    private final ReportFacts facts;
    private final ReportCalculator calculator;
    private final CatalogService catalog;

    ReportService(ReportFacts facts, ReportCalculator calculator, CatalogService catalog) {
        this.facts = facts;
        this.calculator = calculator;
        this.catalog = catalog;
    }

    public TrainingReport training(long userId, ReportSearch search) {
        Range range = Range.of(search);
        List<TrainingReportRow> rows = facts.sessions(userId, range.from(), range.to()).stream()
                .map(calculator::trainingRow)
                .toList();
        return TrainingReport.of(range.from(), range.to(), rows, Instant.now());
    }

    public BalanceReport balance(long userId, ReportSearch search) {
        Range range = Range.of(search);
        List<SessionFacts> sessions = facts.sessions(userId, range.from(), range.to());
        return BalanceReport.of(range.from(), range.to(),
                calculator.muscleBalance(catalog.muscleGroups(), sessions),
                calculator.directionBalance(sessions),
                calculator.pushPullRatio(sessions),
                Instant.now());
    }

    /* defaults to the last 28 days; the client sends its own local dates */
    private record Range(LocalDate from, LocalDate to) {

        static Range of(ReportSearch search) {
            LocalDate to = search.to() != null ? search.to() : LocalDate.now(ZoneOffset.UTC);
            LocalDate from = search.from() != null ? search.from() : to.minusDays(DEFAULT_DAYS - 1);
            Require.that(!from.isAfter(to), "The start date must be on or before the end date");
            Require.that(ChronoUnit.DAYS.between(from, to) < MAX_DAYS, "The range must be " + MAX_DAYS + " days or fewer");
            return new Range(from, to);
        }
    }
}

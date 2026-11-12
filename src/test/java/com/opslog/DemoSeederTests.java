package com.opslog;

import com.opslog.auth.UserAccountRepository;
import com.opslog.demo.DemoSeeder;
import com.opslog.logging.HistoryRow;
import com.opslog.logging.HistorySearch;
import com.opslog.logging.HistoryService;
import com.opslog.report.ReportSearch;
import com.opslog.report.ReportService;
import com.opslog.report.TrainingReport;
import com.opslog.report.TrainingReportRow;
import com.opslog.session.SessionExerciseResponse;
import com.opslog.session.SessionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.*;

import static org.junit.jupiter.api.Assertions.*;

/* not @Transactional: the seeder runs its own transaction */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DemoSeederTests {

    private static final DemoSeeder.Account ACCOUNT = new DemoSeeder.Account("demo-seed@example.com", "password123", "Kai Demo");

    @Autowired DemoSeeder seeder;
    @Autowired UserAccountRepository users;
    @Autowired HistoryService history;
    @Autowired ReportService reports;
    @Autowired SessionService sessions;

    private final LocalDate today = LocalDate.now(ZoneOffset.UTC);

    @AfterEach
    void tearDown() {
        users.findByEmail(ACCOUNT.email()).ifPresent(users::delete);
    }

    private long userId() { return users.findByEmail(ACCOUNT.email()).orElseThrow().getId(); }

    private TrainingReport report() {
        return reports.training(userId(), new ReportSearch(today.minusDays(14), today));
    }

    private List<HistoryRow> rows() {
        return history.search(userId(), new HistorySearch(null, null, today.minusDays(14), today, 0, 100)).items();
    }

    @Test
    void seedsTwoWeeksOfRealisticHistory() {
        assertTrue(seeder.seed(ACCOUNT, today, false));

        TrainingReport report = report();
        // Upper/Lower 4-Day: 8 training days in two weeks
        assertEquals(8, report.sessions());
        assertTrue(report.setsLogged() > 50);
        List<String> tiers = report.rows().stream().map(TrainingReportRow::tier).distinct().toList();
        assertTrue(tiers.containsAll(List.of("Full", "Reduced", "Recovery")), "tiers: " + tiers);
        assertTrue(report.rows().stream().anyMatch(row -> row.location().equals("Kaimana Kapolei")));
        // the report leaves skipped exercises out, so check the stored sessions
        assertTrue(report.rows().stream().map(TrainingReportRow::date)
                .flatMap(date -> sessions.get(userId(), date).exercises().stream())
                .anyMatch(SessionExerciseResponse::skipped), "one skipped exercise");

        List<HistoryRow> rows = rows();
        assertTrue(rows.stream().anyMatch(row -> !row.clean()), "one hard set");
        // the progression rule raised at least one load after clean sets at the top of the range
        Map<Long, Set<BigDecimal>> loads = rows.stream().filter(row -> row.loadLb() != null)
                .collect(groupingBy(HistoryRow::exerciseId, mapping(row -> row.loadLb().stripTrailingZeros(), toSet())));
        assertTrue(loads.values().stream().anyMatch(set -> set.size() > 1), "loads: " + loads);
    }

    @Test
    void existingAccountIsLeftAloneUnlessReset() {
        seeder.seed(ACCOUNT, today, false);
        long first = userId();
        int sets = report().setsLogged();

        assertFalse(seeder.seed(ACCOUNT, today, false));
        assertEquals(first, userId());

        assertTrue(seeder.seed(ACCOUNT, today, true));
        assertNotEquals(first, userId());
        assertEquals(sets, report().setsLogged());
    }
}

package com.opslog.report;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TrainingReport(
        String title,
        Instant generatedAt,
        LocalDate from,
        LocalDate to,
        List<TrainingReportRow> rows,
        int sessions,
        int setsLogged,
        BigDecimal volumeLb
) {
    static TrainingReport of(LocalDate from, LocalDate to, List<TrainingReportRow> rows, Instant generatedAt) {
        int sets = rows.stream().mapToInt(TrainingReportRow::setsLogged).sum();
        BigDecimal volume = rows.stream().map(TrainingReportRow::volumeLb).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new TrainingReport("Training Report", generatedAt, from, to, rows, rows.size(), sets, volume);
    }
}

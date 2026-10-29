package com.opslog.report;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/* pushPullRatio is null when no pull sets were logged */
public record BalanceReport(
        String title,
        Instant generatedAt,
        LocalDate from,
        LocalDate to,
        int totalSets,
        List<MuscleBalanceRow> muscles,
        List<DirectionBalanceRow> directions,
        BigDecimal pushPullRatio
) {
    static BalanceReport of(LocalDate from, LocalDate to, List<MuscleBalanceRow> muscles,
                            List<DirectionBalanceRow> directions, BigDecimal pushPullRatio, Instant generatedAt) {
        int total = muscles.stream().mapToInt(MuscleBalanceRow::sets).sum();
        return new BalanceReport("Training Balance Report", generatedAt, from, to, total, muscles, directions, pushPullRatio);
    }
}

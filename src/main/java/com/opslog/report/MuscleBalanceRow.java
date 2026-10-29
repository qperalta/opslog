package com.opslog.report;

import java.time.LocalDate;

/* lastTrained is null when the muscle had no logged sets in the range */
public record MuscleBalanceRow(String muscle, int sets, int sharePercent, int sessions, LocalDate lastTrained) {}

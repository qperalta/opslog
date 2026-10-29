package com.opslog.report;

import java.math.BigDecimal;
import java.time.LocalDate;

/* cleanPercent is null when nothing was logged */
public record TrainingReportRow(
        LocalDate date,
        String workout,
        String tier,
        String location,
        int exercisesDone,
        int exercisesPlanned,
        int setsLogged,
        Integer cleanPercent,
        BigDecimal volumeLb
) {}

package com.opslog.report;

/* sharePercent is out of push + pull sets only */
public record DirectionBalanceRow(String direction, int sets, int sharePercent, int sessions) {}

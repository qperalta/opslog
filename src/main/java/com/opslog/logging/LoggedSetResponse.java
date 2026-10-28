package com.opslog.logging;

import java.math.BigDecimal;
import java.time.Instant;

public record LoggedSetResponse(int setNumber, Integer reps, Integer durationSeconds, BigDecimal loadLb, boolean clean, Instant loggedAt) {

    public static LoggedSetResponse from(LoggedSet set) {
        return new LoggedSetResponse(set.getSetNumber(), set.getReps(), set.getDurationSeconds(), set.getLoadLb(), set.isClean(),
                set.getLoggedAt());
    }
}

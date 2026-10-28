package com.opslog.logging;

import java.math.BigDecimal;

/* what the progression rule needs from a logged set */
public record SetResult(Integer reps, BigDecimal loadLb, boolean clean) {

    static SetResult from(LoggedSet set) {
        return new SetResult(set.getReps(), set.getLoadLb(), set.isClean());
    }
}

package com.opslog.mcp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CoachSummaryTest {

    @Test void textIsTrimmed() {
        assertEquals("Four days this week", new CoachSummary(7L, "  Four days this week  ").getSummary());
    }

    @Test void rewriteReplacesTheText() {
        CoachSummary summary = new CoachSummary(7L, "Four days this week");

        summary.rewrite("Rest on Wednesday");

        assertEquals("Rest on Wednesday", summary.getSummary());
        assertNotNull(summary.getUpdatedAt());
    }

    @Test void blankIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new CoachSummary(7L, "   "));
        assertThrows(IllegalArgumentException.class, () -> new CoachSummary(7L, null));
    }

    @Test void longerThanTheColumnIsRefused() {
        assertDoesNotThrow(() -> new CoachSummary(7L, "x".repeat(1000)));
        assertThrows(IllegalArgumentException.class, () -> new CoachSummary(7L, "x".repeat(1001)));
    }
}

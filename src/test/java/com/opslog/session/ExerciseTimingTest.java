package com.opslog.session;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExerciseTimingTest {

    @Test void timedExerciseKeepsBothTimes() {
        ExerciseTiming timing = new ExerciseTiming(7L, 3L);

        timing.setTimes(75, 45, true);

        assertEquals(75, timing.getHoldSeconds());
        assertEquals(45, timing.getRestSeconds());
        assertFalse(timing.isEmpty());
    }

    @Test void onlyTimedExercisesHold() {
        ExerciseTiming timing = new ExerciseTiming(7L, 3L);

        assertThrows(IllegalArgumentException.class, () -> timing.setTimes(60, null, false));
        assertDoesNotThrow(() -> timing.setTimes(null, 120, false));
    }

    @Test void limitsMatchTheColumns() {
        ExerciseTiming timing = new ExerciseTiming(7L, 3L);

        assertThrows(IllegalArgumentException.class, () -> timing.setTimes(0, null, true));
        assertThrows(IllegalArgumentException.class, () -> timing.setTimes(601, null, true));
        assertThrows(IllegalArgumentException.class, () -> timing.setTimes(null, 901, true));
    }

    @Test void bothNullIsEmpty() {
        ExerciseTiming timing = new ExerciseTiming(7L, 3L);

        timing.setTimes(null, null, true);

        assertTrue(timing.isEmpty());
    }
}

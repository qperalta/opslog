package com.opslog.auth;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptsTest {

    /* a clock the test moves forward by hand */
    static class TestClock extends Clock {
        private Instant now = Instant.parse("2026-11-12T08:00:00Z");
        void advance(Duration duration) { now = now.plus(duration); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private final TestClock clock = new TestClock();
    private final LoginAttempts attempts = new LoginAttempts(clock);

    private void fail(String email, int times) {
        for(int i = 0; i < times; i++) attempts.recordFailure(email);
    }

    @Test
    void fourFailuresStillAllowSignIn() {
        fail("kai@example.com", 4);

        assertDoesNotThrow(() -> attempts.checkAllowed("kai@example.com"));
    }

    @Test
    void fiveFailuresLockTheEmailForFifteenMinutes() {
        fail("kai@example.com", 5);

        var ex = assertThrows(TooManyLoginAttemptsException.class, () -> attempts.checkAllowed("kai@example.com"));
        assertEquals("Too many failed sign-ins. Try again in 15 minutes.", ex.getMessage());
        assertEquals(Duration.ofMinutes(15), ex.getRetryAfter());
        assertEquals(900, ex.getRetryAfterSeconds());

        clock.advance(Duration.ofMinutes(14).plusSeconds(30));
        assertEquals("Too many failed sign-ins. Try again in 1 minute.",
                assertThrows(TooManyLoginAttemptsException.class, () -> attempts.checkAllowed("kai@example.com")).getMessage());

        clock.advance(Duration.ofSeconds(30));
        assertDoesNotThrow(() -> attempts.checkAllowed("kai@example.com"));
    }

    @Test
    void failuresOutsideTheWindowStartOver() {
        fail("kai@example.com", 4);
        clock.advance(Duration.ofMinutes(15));
        fail("kai@example.com", 1);

        assertDoesNotThrow(() -> attempts.checkAllowed("kai@example.com"));
    }

    @Test
    void successClearsTheCount() {
        fail("kai@example.com", 4);
        attempts.recordSuccess("kai@example.com");
        fail("kai@example.com", 4);

        assertDoesNotThrow(() -> attempts.checkAllowed("kai@example.com"));
    }

    @Test
    void oneEmailsLockDoesNotAffectAnother() {
        fail("kai@example.com", 5);

        assertDoesNotThrow(() -> attempts.checkAllowed("lani@example.com"));
    }
}

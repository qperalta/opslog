package com.opslog.auth;

import java.time.Duration;

public class TooManyLoginAttemptsException extends RuntimeException {

    private final Duration retryAfter;

    public TooManyLoginAttemptsException(Duration retryAfter) {
        super("Too many failed sign-ins. Try again in " + minutes(retryAfter) + " minute" + (minutes(retryAfter) == 1 ? "" : "s") + ".");
        this.retryAfter = retryAfter;
    }

    public Duration getRetryAfter() { return retryAfter; }

    /* rounded up for the Retry-After header */
    public long getRetryAfterSeconds() { return (retryAfter.toMillis() + 999) / 1000; }

    /* rounded up, so "0 minutes" is never shown */
    private static long minutes(Duration duration) {
        return Math.max(1, (duration.toSeconds() + 59) / 60);
    }
}

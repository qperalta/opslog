package com.opslog.auth;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/* slows password guessing: 5 failed sign-ins for one email within 15 minutes locks that email for 15 minutes
 *  in memory, which fits one instance; more instances would need a shared store (Redis or a table) */
@Component
public class LoginAttempts {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);
    static final Duration LOCK = Duration.ofMinutes(15);
    /* stops a flood of made-up emails from growing the map without limit */
    static final int MAX_TRACKED = 10_000;

    private record Failures(int count, Instant firstAt, Instant lockedUntil) {}

    private final Clock clock;
    private final Map<String, Failures> failures = new ConcurrentHashMap<>();

    public LoginAttempts() { this(Clock.systemUTC()); }

    LoginAttempts(Clock clock) { this.clock = clock; }

    /* throws while the email is locked */
    public void checkAllowed(String email) {
        Failures entry = failures.get(email);
        Instant now = clock.instant();
        if(entry != null && entry.lockedUntil() != null && now.isBefore(entry.lockedUntil())) {
            throw new TooManyLoginAttemptsException(Duration.between(now, entry.lockedUntil()));
        }
    }

    public void recordFailure(String email) {
        Instant now = clock.instant();
        if(failures.size() >= MAX_TRACKED) failures.values().removeIf(entry -> expired(entry, now));
        failures.compute(email, (key, entry) -> {
            if(entry == null || expired(entry, now)) return new Failures(1, now, null);
            int count = entry.count() + 1;
            return new Failures(count, entry.firstAt(), count >= MAX_FAILURES ? now.plus(LOCK) : null);
        });
    }

    public void recordSuccess(String email) { failures.remove(email); }

    /* a finished lock or a window with no lock starts over */
    private static boolean expired(Failures entry, Instant now) {
        return entry.lockedUntil() != null
                ? !now.isBefore(entry.lockedUntil())
                : !now.isBefore(entry.firstAt().plus(WINDOW));
    }
}

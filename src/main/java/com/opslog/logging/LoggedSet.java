package com.opslog.logging;

import com.opslog.catalog.Exercise;
import com.opslog.catalog.StrengthExercise;
import com.opslog.common.Require;
import com.opslog.session.SessionExercise;
import jakarta.persistence.*;
import org.hibernate.Hibernate;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name="logged_sets")
public class LoggedSet {

    public static final int MAX_SETS = 10;
    private static final int MAX_REPS = 100;
    private static final int MAX_SECONDS = 7200;

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="session_exercise_id")
    private SessionExercise sessionExercise;

    @Column(name="set_number", nullable=false, updatable=false)
    private int setNumber;

    /* strength only */
    private Integer reps;

    /* timed and cardio only */
    @Column(name="duration_seconds")
    private Integer durationSeconds;

    /* null = bodyweight or not recorded */
    @Column(name="load_lb", precision=6, scale=2)
    private BigDecimal loadLb;

    @Column(nullable=false)
    private boolean clean;

    @Column(name="logged_at", nullable=false, updatable=false)
    private Instant loggedAt;

    protected LoggedSet() {}

    public LoggedSet(SessionExercise sessionExercise, int setNumber, Integer reps, Integer durationSeconds, BigDecimal loadLb, boolean clean) {
        Require.that(sessionExercise != null, "Session exercise is required");
        Require.that(setNumber >= 1 && setNumber <= MAX_SETS, "Set number must be between 1 and " + MAX_SETS);
        this.sessionExercise = sessionExercise;
        this.setNumber = setNumber;
        this.loggedAt = Instant.now();
        record(reps, durationSeconds, loadLb, clean);
    }

    public Long getId() { return id; }
    public SessionExercise getSessionExercise() { return sessionExercise; }
    public int getSetNumber() { return setNumber; }
    public Integer getReps() { return reps; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public BigDecimal getLoadLb() { return loadLb; }
    public boolean isClean() { return clean; }
    public Instant getLoggedAt() { return loggedAt; }

    /* reps for strength, seconds for everything else */
    public void record(Integer reps, Integer durationSeconds, BigDecimal loadLb, boolean clean) {
        boolean strength = Hibernate.unproxy(sessionExercise.getExercise(), Exercise.class) instanceof StrengthExercise;
        if(strength) {
            Require.that(reps != null, "Reps are required for a strength set");
            Require.that(durationSeconds == null, "A strength set logs reps, not seconds");
            Require.that(reps >= 0 && reps <= MAX_REPS, "Reps must be between 0 and " + MAX_REPS);
        } else {
            Require.that(durationSeconds != null, "Seconds are required for a timed or cardio set");
            Require.that(reps == null, "A timed or cardio set logs seconds, not reps");
            Require.that(durationSeconds >= 1 && durationSeconds <= MAX_SECONDS, "Seconds must be between 1 and " + MAX_SECONDS);
        }
        Require.that(loadLb == null || loadLb.signum() >= 0, "Load cannot be negative");
        this.reps = reps;
        this.durationSeconds = durationSeconds;
        this.loadLb = loadLb;
        this.clean = clean;
    }
}

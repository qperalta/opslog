package com.opslog.session;

import com.opslog.catalog.Exercise;
import com.opslog.common.Require;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name="session_exercises")
public class SessionExercise {

    private static final int MAX_SETS = 10;
    private static final int MAX_REASON = 200;

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="session_id")
    private TrainingSession session;

    @Column(nullable=false)
    private int position;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="exercise_id")
    private Exercise exercise;

    /* null = the template exercise as planned */
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="substituted_for_id")
    private Exercise substitutedFor;

    @Column(nullable=false)
    private int sets;

    /* null = no load yet or not loadable */
    @Column(name="target_load_lb", precision=6, scale=2)
    private BigDecimal targetLoadLb;

    @Column(name="main_lift", nullable=false)
    private boolean mainLift;

    @Column(length=MAX_REASON)
    private String reason;

    protected SessionExercise() {}

    SessionExercise(TrainingSession session, int position, Exercise exercise, Exercise substitutedFor, int sets, BigDecimal targetLoadLb, boolean mainLift, String reason) {
        Require.that(session != null, "Session is required");
        Require.that(position > 0, "Position must be greater than zero");
        Require.that(exercise != null, "Exercise is required");
        Require.that(sets >= 1 && sets <= MAX_SETS, "Sets must be between 1 and " + MAX_SETS);
        Require.that(targetLoadLb == null || targetLoadLb.signum() >= 0, "Target load cannot be negative");
        Require.that(reason == null || reason.length() <= MAX_REASON, "Reason must be " + MAX_REASON + " characters or fewer");
        this.session = session;
        this.position = position;
        this.exercise = exercise;
        this.substitutedFor = substitutedFor;
        this.sets = sets;
        this.targetLoadLb = targetLoadLb;
        this.mainLift = mainLift;
        this.reason = reason;
    }

    public Long getId() { return id; }
    public TrainingSession getSession() { return session; }
    public int getPosition() { return position; }
    public Exercise getExercise() { return exercise; }
    public Exercise getSubstitutedFor() { return substitutedFor; }
    public int getSets() { return sets; }
    public BigDecimal getTargetLoadLb() { return targetLoadLb; }
    public boolean isMainLift() { return mainLift; }
    public String getReason() { return reason; }

    public boolean isSubstitute() { return substitutedFor != null; }
}

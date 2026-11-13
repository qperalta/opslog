package com.opslog.session;

import com.opslog.catalog.Exercise;
import com.opslog.common.Require;
import com.opslog.logging.LoggedSet;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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

    @Column(nullable=false)
    private boolean skipped;

    /* inserted by the member during the workout */
    @Column(name="ad_hoc", nullable=false, updatable=false)
    private boolean adHoc;

    @OneToMany(mappedBy="sessionExercise", cascade=CascadeType.ALL, orphanRemoval=true)
    @OrderBy("setNumber")
    private List<LoggedSet> loggedSets = new ArrayList<>();

    protected SessionExercise() {}

    SessionExercise(TrainingSession session, int position, Exercise exercise, Exercise substitutedFor, int sets, BigDecimal targetLoadLb,
                    boolean mainLift, String reason, boolean adHoc) {
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
        this.adHoc = adHoc;
    }

    public Long getId() { return id; }
    public TrainingSession getSession() { return session; }
    public int getPosition() { return position; }
    public Exercise getExercise() { return exercise; }
    public Exercise getSubstitutedFor() { return substitutedFor; }
    public int getSets() { return sets; }
    public BigDecimal getTargetLoadLb() { return targetLoadLb; }
    public boolean isMainLift() { return mainLift; }
    public boolean isSkipped() { return skipped; }
    public boolean isAdHoc() { return adHoc; }
    public String getReason() { return reason; }

    public List<LoggedSet> getLoggedSets() { return Collections.unmodifiableList(loggedSets); }

    public boolean isSubstitute() { return substitutedFor != null; }

    public boolean hasLoggedSets() { return !loggedSets.isEmpty(); }

    /* a skipped exercise has nothing logged, so the reports never count half of one */
    public void skip() {
        Require.that(loggedSets.isEmpty(), "Delete the logged sets before skipping this exercise");
        skipped = true;
    }

    public void reactivate() { skipped = false; }

    /* an exercise was inserted above this one */
    void moveDown() { position++; }

    /* numbers continue after the highest one, so a deleted set leaves a gap */
    public LoggedSet logSet(Integer reps, Integer durationSeconds, BigDecimal loadLb, boolean clean) {
        Require.that(!skipped, "Re-activate this exercise before logging a set");
        int next = loggedSets.stream().mapToInt(LoggedSet::getSetNumber).max().orElse(0) + 1;
        Require.that(next <= LoggedSet.MAX_SETS, "An exercise can have at most " + LoggedSet.MAX_SETS + " logged sets");
        LoggedSet set = new LoggedSet(this, next, reps, durationSeconds, loadLb, clean);
        loggedSets.add(set);
        return set;
    }

    public Optional<LoggedSet> findLoggedSet(int setNumber) {
        return loggedSets.stream().filter(set -> set.getSetNumber() == setNumber).findFirst();
    }

    public void removeLoggedSet(LoggedSet set) { loggedSets.remove(set); }
}

package com.opslog.session;

import com.opslog.catalog.Exercise;
import com.opslog.checkin.TrainingTier;
import com.opslog.common.Require;
import com.opslog.location.Location;
import com.opslog.plan.Actor;
import com.opslog.plan.TemplateDay;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* one per user per day */
@Entity
@Table(name="training_sessions")
public class TrainingSession {

    private static final int MAX_RATIONALE = 1000;

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_id", nullable=false, updatable=false)
    private Long userId;

    @Column(name="session_date", nullable=false, updatable=false)
    private LocalDate date;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="template_day_id")
    private TemplateDay workout;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="tier_id")
    private TrainingTier tier;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="location_id")
    private Location location;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=10)
    private Actor source;

    @Column(length=MAX_RATIONALE)
    private String rationale;

    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt;

    @OneToMany(mappedBy="session", cascade=CascadeType.ALL, orphanRemoval=true)
    @OrderBy("position")
    private List<SessionExercise> exercises = new ArrayList<>();

    protected TrainingSession() {}

    public TrainingSession(Long userId, LocalDate date, TemplateDay workout, TrainingTier tier, Location location, Actor source, String rationale) {
        Require.that(userId != null, "User is required");
        Require.that(date != null, "Session date is required");
        Require.that(tier != null, "Training tier is required");
        Require.that(source != null, "Source is required");
        Require.that(rationale == null || rationale.length() <= MAX_RATIONALE, "Rationale must be " + MAX_RATIONALE + " characters or fewer");
        this.userId = userId;
        this.date = date;
        this.workout = workout;
        this.tier = tier;
        this.location = location;
        this.source = source;
        this.rationale = rationale;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public LocalDate getDate() { return date; }
    public TemplateDay getWorkout() { return workout; }
    public TrainingTier getTier() { return tier; }
    public Location getLocation() { return location; }
    public Actor getSource() { return source; }
    public String getRationale() { return rationale; }
    public Instant getCreatedAt() { return createdAt; }
    public List<SessionExercise> getExercises() { return Collections.unmodifiableList(exercises); }

    public SessionExercise addExercise(Exercise exercise, Exercise substitutedFor, int sets, BigDecimal targetLoadLb, boolean mainLift, String reason) {
        SessionExercise item = new SessionExercise(this, exercises.size() + 1, exercise, substitutedFor, sets, targetLoadLb, mainLift, reason);
        exercises.add(item);
        return item;
    }
}

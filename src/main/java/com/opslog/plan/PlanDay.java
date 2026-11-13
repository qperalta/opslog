package com.opslog.plan;

import com.opslog.common.Require;
import com.opslog.location.Location;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/* one per user per day; workout null = rest */
@Entity
@Table(name="plan_days")
public class PlanDay {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_id", nullable=false, updatable=false)
    private Long userId;

    @Column(name="plan_date", nullable=false, updatable=false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=10)
    private PlanMode mode;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="template_day_id")
    private TemplateDay workout;

    /* null = the program's default location */
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="location_id")
    private Location location;

    @Column(nullable=false)
    private boolean locked;

    @Enumerated(EnumType.STRING)
    @Column(name="updated_by", nullable=false, length=10)
    private Actor updatedBy;

    @Column(name="updated_at", nullable=false)
    private Instant updatedAt;

    protected PlanDay() {}

    public PlanDay(Long userId, LocalDate date) {
        Require.that(userId != null, "User is required");
        Require.that(date != null, "Plan date is required");
        this.userId = userId;
        this.date = date;
        this.mode = PlanMode.AUTO;
        this.updatedBy = Actor.RULES;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public LocalDate getDate() { return date; }
    public PlanMode getMode() { return mode; }
    public TemplateDay getWorkout() { return workout; }
    public Location getLocation() { return location; }
    public boolean isLocked() { return locked; }
    public Actor getUpdatedBy() { return updatedBy; }
    public Instant getUpdatedAt() { return updatedAt; }

    public boolean isTraining() { return workout != null; }

    /* a Train day left on "Next in rotation"; the rules engine fills it even when locked */
    public boolean needsWorkout() { return mode == PlanMode.TRAINING && workout == null; }

    /* user or agent edit; the locked-day guard lives in PlanDayService */
    public void edit(PlanMode mode, TemplateDay workout, Location location, boolean locked, Actor actor) {
        Require.that(mode != null, "Mode is required");
        Require.that(actor != null && actor != Actor.RULES, "Only the user or the agent can edit a day");
        Require.that(mode != PlanMode.REST || workout == null, "A rest day cannot have a workout");
        this.mode = mode;
        this.workout = workout;
        this.location = location;
        this.locked = locked;
        touch(actor);
    }

    /* rules engine only */
    public void schedule(TemplateDay workout) {
        Require.that(!locked || needsWorkout(), "A locked day cannot be re-planned");
        Require.that(mode != PlanMode.REST || workout == null, "A rest day cannot have a workout");
        if(Objects.equals(this.workout, workout)) return;
        this.workout = workout;
        // a locked day stays "set by you"
        if(!locked) touch(Actor.RULES);
    }

    private void touch(Actor actor) {
        this.updatedBy = actor;
        this.updatedAt = Instant.now();
    }
}

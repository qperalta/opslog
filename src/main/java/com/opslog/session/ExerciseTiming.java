package com.opslog.session;

import com.opslog.common.Require;
import jakarta.persistence.*;

import java.time.Instant;

/* the member's own hold and rest times for one exercise; null = the default */
@Entity
@Table(name="exercise_timings")
public class ExerciseTiming {

    static final int MAX_HOLD = 600;
    static final int MAX_REST = 900;

    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="user_id", nullable = false, updatable = false) private Long userId;
    @Column(name="exercise_id", nullable = false, updatable = false) private Long exerciseId;
    @Column(name="hold_seconds") private Integer holdSeconds;
    @Column(name="rest_seconds") private Integer restSeconds;
    @Column(name="updated_at", nullable = false) private Instant updatedAt;

    protected ExerciseTiming() {}

    public ExerciseTiming(Long userId, Long exerciseId) {
        Require.that(userId != null, "User is required");
        Require.that(exerciseId != null, "Exercise is required");
        this.userId = userId;
        this.exerciseId = exerciseId;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getExerciseId() { return exerciseId; }
    public Integer getHoldSeconds() { return holdSeconds; }
    public Integer getRestSeconds() { return restSeconds; }
    public Instant getUpdatedAt() { return updatedAt; }

    // only timed exercises hold; strength and cardio sets are counted, not timed
    public void setTimes(Integer holdSeconds, Integer restSeconds, boolean timed) {
        Require.that(holdSeconds == null || timed, "Only timed exercises have a hold timer");
        Require.that(holdSeconds == null || (holdSeconds >= 1 && holdSeconds <= MAX_HOLD), "Timer must be 0:01 to 10:00");
        Require.that(restSeconds == null || (restSeconds >= 0 && restSeconds <= MAX_REST), "Rest must be 0:00 to 15:00");
        this.holdSeconds = holdSeconds;
        this.restSeconds = restSeconds;
        this.updatedAt = Instant.now();
    }

    public boolean isEmpty() { return holdSeconds == null && restSeconds == null; }
}

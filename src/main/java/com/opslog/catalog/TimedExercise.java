package com.opslog.catalog;

import com.opslog.common.Require;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("TIMED")
public class TimedExercise extends Exercise{

    private static final int MAX_SECONDS = 600;

    @Column(name="target_seconds")
    private Integer targetSeconds;

    protected TimedExercise() {}

    public TimedExercise(String name, MovementPattern movementPattern, MuscleGroup primaryMuscle, Long ownerId, int targetSeconds) {
        super(name, movementPattern, primaryMuscle, ownerId);
        setTargetSeconds(targetSeconds);
    }

    public Integer getTargetSeconds() { return targetSeconds; }

    public void setTargetSeconds(int targetSeconds) {
        Require.that(targetSeconds > 0, "Target time must be greater than zero");
        Require.that(targetSeconds <= MAX_SECONDS, "Target time must be " + MAX_SECONDS + " seconds or less");
        this.targetSeconds = targetSeconds;
    }

    @Override
    public String describeTarget() { return targetSeconds + " s"; }
}

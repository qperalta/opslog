package com.opslog.catalog;

import com.opslog.common.Require;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("CARDIO")
public class CardioFinisher extends Exercise{

    private static final int MAX_MINUTES = 60;

    @Column(name="duration_minutes")
    private Integer durationMinutes;

    protected CardioFinisher() {}

    public CardioFinisher(String name, MovementPattern movementPattern, MuscleGroup primaryMuscle, Long ownerId, int durationMinutes) {
        super(name, movementPattern, primaryMuscle, ownerId);
        setDurationMinutes(durationMinutes);
    }

    public Integer getDurationMinutes() { return durationMinutes; }

    public void setDurationMinutes(int durationMinutes) {
        Require.that(durationMinutes > 0, "Duration must be greater than zero");
        Require.that(durationMinutes <= MAX_MINUTES, "Duration must be " + MAX_MINUTES + " minutes or less");
        this.durationMinutes = durationMinutes;
    }

    @Override
    public String describeTarget() { return durationMinutes + " min"; }
}

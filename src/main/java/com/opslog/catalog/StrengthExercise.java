package com.opslog.catalog;

import com.opslog.common.Require;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("STRENGTH")
public class StrengthExercise extends Exercise{

    private static final int MAX_REPS = 50;

    @Column(name="rep_min")
    private Integer repMin;

    @Column(name="rep_max")
    private Integer repMax;

    protected StrengthExercise() {}

    public StrengthExercise(String name, MovementPattern movementPattern, MuscleGroup primaryMuscle, Long ownerId, int repMin, int repMax) {
        super(name, movementPattern, primaryMuscle, ownerId);
        setRepRange(repMin, repMax);
    }

    public Integer getRepMin() { return repMin; }
    public Integer getRepMax() { return repMax; }

    public void setRepRange(int repMin, int repMax) {
        Require.that(repMin > 0, "Minimum reps must be greater than zero");
        Require.that(repMax >= repMin, "Maximum reps must be at least the minimum");
        Require.that(repMax <= MAX_REPS, "Maximum reps must be " + MAX_REPS + " or fewer");
        this.repMin = repMin;
        this.repMax = repMax;
    }

    @Override
    public String describeTarget() {
        return repMin.equals(repMax)
                ? repMin + " reps"
                : repMin + "-" + repMax + " reps";
    }
}

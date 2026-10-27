package com.opslog.checkin;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.math.BigDecimal;

@Entity
@DiscriminatorValue("REDUCED")
public class ReducedTier extends TrainingTier{

    protected ReducedTier() {}

    public ReducedTier(String name, String description, int intensity, int minSleep, int maxSoreness) {
        super(name, description, intensity, minSleep, maxSoreness);
    }

    @Override protected int adjustSets(int plannedSets, boolean mainLift) { return mainLift ? plannedSets : Math.max(1, plannedSets - 1); }

    @Override protected BigDecimal adjustLoad(BigDecimal plannedLoad) { return plannedLoad; }
}

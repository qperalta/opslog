package com.opslog.checkin;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.math.BigDecimal;

@Entity
@DiscriminatorValue("FULL")
public class FullTier extends TrainingTier{

    protected FullTier() {}

    public FullTier(String name, String description, int intensity, int minSleep, int maxSoreness) {
        super(name, description, intensity, minSleep, maxSoreness);
    }

    @Override protected int adjustSets(int plannedSets, boolean mainLift) { return plannedSets; }
    @Override protected BigDecimal adjustLoad(BigDecimal plannedLoad) { return plannedLoad; }
}

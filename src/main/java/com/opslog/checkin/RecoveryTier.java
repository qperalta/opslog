package com.opslog.checkin;

import com.opslog.common.Require;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@DiscriminatorValue("RECOVERY")
public class RecoveryTier extends TrainingTier{

    @Column(name="load_factor", precision=3, scale=2)
    private BigDecimal loadFactor;

    protected RecoveryTier() {}

    public RecoveryTier(String name, String description, int intensity, int minSleep, int maxSoreness, BigDecimal loadFactor) {
        super(name, description, intensity, minSleep, maxSoreness);
        Require.that(loadFactor != null && loadFactor.signum() > 0 && loadFactor.compareTo(BigDecimal.ONE) < 0,
                "Load factor must be between 0 and 1");
        this.loadFactor = loadFactor;
    }

    public BigDecimal getLoadFactor() { return loadFactor; }

    @Override protected int adjustSets(int plannedSets, boolean mainLift) { return (plannedSets + 1) / 2; }

    @Override protected BigDecimal adjustLoad(BigDecimal plannedLoad) { return plannedLoad.multiply(loadFactor).setScale(2, RoundingMode.HALF_UP); }
}

package com.opslog.checkin;

import com.opslog.common.Require;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name="training_tiers")
@Inheritance(strategy= InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="kind", length=20)
public abstract class TrainingTier {

    private static final int MIN_SCORE = 1;
    private static final int MAX_SCORE = 5;

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length=40)
    private String name;

    @Column(nullable=false, length=200)
    private String description;

    @Column(nullable=false)
    private int intensity;

    @Column(name="min_sleep", nullable = false)
    private int minSleep;

    @Column(name="max_soreness", nullable=false)
    private int maxSoreness;

    protected TrainingTier() {}

    protected TrainingTier(String name, String description, int intensity, int minSleep, int maxSoreness) {
        Require.that(name != null && !name.isBlank(), "Tier name is required");
        Require.that(description != null && !description.isBlank(), "Tier description is required");
        requireScore(minSleep, "Minimum sleep");
        requireScore(maxSoreness, "Maximum soreness");
        this.name = name.trim();
        this.description = description.trim();
        this.intensity = intensity;
        this.minSleep = minSleep;
        this.maxSoreness = maxSoreness;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getIntensity() { return intensity; }
    public int getMinSleep() { return minSleep; }
    public int getMaxSoreness() { return maxSoreness; }

    public boolean accepts(int sleepQuality, int soreness) { return sleepQuality >= minSleep && soreness <= maxSoreness; }

    public final int scaleSets(int plannedSets, boolean mainLift) {
        Require.that(plannedSets > 0, "Planned sets must be greater than zero");
        return adjustSets(plannedSets, mainLift);
    }

    /* null = no load (bw/timed) */
    public final BigDecimal scaleLoad(BigDecimal plannedLoad) {
        if(plannedLoad == null) return null;
        Require.that(plannedLoad.signum() >= 0, "Planned load cannot be negative");
        return adjustLoad(plannedLoad);
    }

    protected abstract int adjustSets(int plannedSets, boolean mainLift);

    protected abstract BigDecimal adjustLoad(BigDecimal plannedLoad);

    static void requireScore(int score, String label) {
        Require.that(score >= MIN_SCORE && score <= MAX_SCORE, label + " must be between " + MIN_SCORE + " and " + MAX_SCORE);
    }
}

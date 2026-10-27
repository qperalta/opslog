package com.opslog.checkin;

import com.opslog.common.Require;
import com.opslog.location.Location;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

/* one per user per day */
@Entity
@Table(name="check_ins")
public class CheckIn {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_id", nullable=false, updatable=false)
    private Long userId;

    @Column(name="check_in_date", nullable=false, updatable=false)
    private LocalDate date;

    @Column(name="sleep_quality", nullable=false)
    private int sleepQuality;

    @Column(nullable=false)
    private int soreness;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="location_id")
    private Location location;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="tier_id")
    private TrainingTier tier;

    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt;

    @Column(name="updated_at", nullable=false)
    private Instant updatedAt;

    protected CheckIn() {}

    public CheckIn(Long userId, LocalDate date) {
        Require.that(userId != null, "User is required");
        Require.that(date != null, "Check-in date is required");
        this.userId = userId;
        this.date = date;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public LocalDate getDate() { return date; }
    public int getSleepQuality() { return sleepQuality; }
    public int getSoreness() { return soreness; }
    public Location getLocation() { return location; }
    public TrainingTier getTier() { return tier; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void record(int sleepQuality, int soreness, Location location, TrainingTier tier) {
        TrainingTier.requireScore(sleepQuality, "Sleep quality");
        TrainingTier.requireScore(soreness, "Soreness");
        Require.that(location != null, "Location is required");
        Require.that(tier != null, "Training tier is required");
        this.sleepQuality = sleepQuality;
        this.soreness = soreness;
        this.location = location;
        this.tier = tier;
        this.updatedAt = Instant.now();
    }
}

package com.opslog.catalog;

import com.opslog.common.Require;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name="exercises")
@Inheritance(strategy= InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="kind", length=20)
public abstract class Exercise {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=80)
    private String name;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="movement_pattern_id")
    private MovementPattern movementPattern;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="primary_muscle_id")
    private MuscleGroup primaryMuscle;

    @Column(name="owner_id", updatable=false)
    private Long ownerId;

    @ManyToMany
    @JoinTable(name="exercise_equipment",
            joinColumns=@JoinColumn(name="exercise_id"),
            inverseJoinColumns=@JoinColumn(name="equipment_id"))
    private Set<Equipment> equipment = new HashSet<>();

    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt;

    protected Exercise() {}

    protected Exercise(String name, MovementPattern movementPattern, MuscleGroup primaryMuscle, Long ownerId) {
        setName(name);
        setMovementPattern(movementPattern);
        setPrimaryMuscle(primaryMuscle);
        this.ownerId = ownerId;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public MovementPattern getMovementPattern() { return movementPattern; }
    public MuscleGroup getPrimaryMuscle() { return primaryMuscle; }
    public Long getOwnerId() { return ownerId; }
    public Set<Equipment> getEquipment() { return Collections.unmodifiableSet(equipment); }
    public Instant getCreatedAt() { return createdAt; }

    public void setName(String name) {
        Require.that(name != null && !name.isBlank(), "Exercise name cannot be null or empty");
        String trimmedName = name.trim();
        Require.that(trimmedName.length() <= 80, "Exercise name must be 80 characters or fewer");
        this.name = trimmedName;
    }

    public void setMovementPattern(MovementPattern movementPattern) {
        Require.that(movementPattern != null, "Movement pattern is required");
        this.movementPattern = movementPattern;
    }

    public void setPrimaryMuscle(MuscleGroup primaryMuscle) {
        Require.that(primaryMuscle != null, "Primary muscle group is required");
        this.primaryMuscle = primaryMuscle;
    }

    public void addEquipment(Equipment item) {
        Require.that(item != null, "Equipment cannot be null");
        equipment.add(item);
    }

    public void removeEquipment(Equipment item) { equipment.remove(item); }

    public boolean isCustom() { return ownerId != null; }

    public boolean isVisibleTo(Long userId) { return ownerId == null || Objects.equals(ownerId, userId); }

    public abstract String describeTarget();
}

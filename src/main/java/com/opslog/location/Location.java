package com.opslog.location;

import com.opslog.catalog.Equipment;
import com.opslog.common.Require;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name="locations")
public class Location {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=60)
    private String name;

    @Column(name="owner_id", updatable=false)
    private Long ownerId;

    @ManyToMany
    @JoinTable(name="location_equipment",
            joinColumns=@JoinColumn(name="location_id"),
            inverseJoinColumns=@JoinColumn(name="equipment_id"))
    private Set<Equipment> equipment = new HashSet<>();

    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt;

    protected Location() {}

    public Location(String name, Long ownerId) {
        setName(name);
        this.ownerId = ownerId;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public Long getOwnerId() { return ownerId; }
    public Set<Equipment> getEquipment() { return Collections.unmodifiableSet(equipment); }
    public Instant getCreatedAt() { return createdAt; }

    public void setName(String name) {
        Require.that(name != null && !name.isBlank(), "Location name cannot be null or empty");
        String trimmedName = name.trim();
        Require.that(trimmedName.length() <= 60, "Location name must be 60 characters or fewer");
        this.name = trimmedName;
    }

    public void replaceEquipment(Collection<Equipment> items) {
        Require.that(items != null && items.stream().noneMatch(Objects::isNull), "Equipment cannot be null");
        equipment.clear();
        equipment.addAll(items);
    }

    /* used by substitution */
    public boolean hasAll(Collection<Equipment> required) { return equipment.containsAll(required); }

    public boolean has(Equipment item) { return equipment.contains(item); }

    public boolean isCustom() { return ownerId != null; }

    public boolean isVisibleTo(Long userId) { return ownerId == null || Objects.equals(ownerId, userId); }
}

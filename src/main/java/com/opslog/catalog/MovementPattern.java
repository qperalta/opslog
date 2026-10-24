package com.opslog.catalog;

import com.opslog.common.Require;
import jakarta.persistence.*;

@Entity
@Table(name="movement_patterns")
public class MovementPattern {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true, length=40)
    private String name;

    protected MovementPattern() {}

    public MovementPattern(String name) { setName(name); }

    public Long getId() { return id; }
    public String getName() { return name; }

    public void setName(String name) {
        Require.that(name != null && !name.isBlank(), "Movement pattern name cannot be null or empty");
        String trimmedName = name.trim();
        Require.that(trimmedName.length() <= 40, "Movement pattern name must be 40 characters or fewer");
        this.name = trimmedName;
    }
}

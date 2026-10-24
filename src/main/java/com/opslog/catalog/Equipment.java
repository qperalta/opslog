package com.opslog.catalog;

import com.opslog.common.Require;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "equipment")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "kind", length = 20)
public abstract class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    protected Equipment() {}

    protected Equipment(String name) {
        setName(name);
    }

    public Long getId() { return id; }
    public String getName() { return name; }

    public void setName(String name) {
        Require.that(name != null && !name.isBlank(), "Equipment name cannot be null or empty");
        String trimmedName = name.trim();
        Require.that(trimmedName.length() <= 80, "Equipment name must be 80 characters or fewer");
        this.name = trimmedName;
    }

    public abstract BigDecimal getLoadIncrementLb();

    public abstract String progressionHint();

    public boolean isLoadable() {
        return getLoadIncrementLb().signum() > 0;
    }

    public BigDecimal nextLoad(BigDecimal currentLb) {
        Require.that(currentLb != null && currentLb.signum() >= 0, "Current load must be zero or more");
        return currentLb.add(getLoadIncrementLb());
    }

    protected static BigDecimal requireLoad(BigDecimal value, BigDecimal max, String label) {
        Require.that(value != null && value.signum() > 0, label + " must be greater than zero");
        Require.that(value.compareTo(max) <= 0, label + " must be " + max + " lb or less");
        Require.that(value.stripTrailingZeros().scale() <= 2, label + " can have at most 2 decimal places");
        return value;
    }
}

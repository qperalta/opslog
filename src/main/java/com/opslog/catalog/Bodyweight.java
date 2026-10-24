package com.opslog.catalog;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.math.BigDecimal;

@Entity
@DiscriminatorValue("BODYWEIGHT")
public class Bodyweight extends Equipment{

    protected Bodyweight() {}

    public Bodyweight(String name) { super(name); }

    @Override public BigDecimal getLoadIncrementLb() { return BigDecimal.ZERO; }
    @Override public String progressionHint() { return "Add reps, then slow the tempo"; }
}

package com.opslog.catalog;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.math.BigDecimal;

@Entity
@DiscriminatorValue("ACCESSORY")
public class Accessory extends Equipment{

    protected Accessory() {}

    public Accessory(String name) { super(name); }

    @Override public BigDecimal getLoadIncrementLb() { return BigDecimal.ZERO; }
    @Override public String progressionHint() { return "Support the lift; progress the main equipment"; }
}

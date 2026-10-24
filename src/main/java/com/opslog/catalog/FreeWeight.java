package com.opslog.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.math.BigDecimal;

@Entity
@DiscriminatorValue("FREE_WEIGHT")
public class FreeWeight extends Equipment{

    private static final BigDecimal MAX_INCREMENT_LB = new BigDecimal("999.99");

    @Column(name = "load_increment_lb", precision = 5, scale = 2)
    private BigDecimal loadIncrementLb;

    protected FreeWeight() {}

    public FreeWeight(String name, BigDecimal loadIncrementLb) {
        super(name);
        setLoadIncrementLb(loadIncrementLb);
    }

    @Override
    public BigDecimal getLoadIncrementLb() { return loadIncrementLb; }

    public void setLoadIncrementLb(BigDecimal loadIncrementLb) {
        this.loadIncrementLb = requireLoad(loadIncrementLb, MAX_INCREMENT_LB, "Load increment");
    }

    @Override
    public String progressionHint() {
        return "Add " + loadIncrementLb.stripTrailingZeros().toPlainString() + " lb";
    }
}

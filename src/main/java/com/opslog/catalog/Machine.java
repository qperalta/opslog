package com.opslog.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.math.BigDecimal;

@Entity
@DiscriminatorValue("MACHINE")
public class Machine extends Equipment{

    private static final BigDecimal MAX_INCREMENT_LB = new BigDecimal("999.99");
    private static final BigDecimal MAX_STACK_LB = new BigDecimal("9999.99");

    @Column(name="load_increment_lb", precision=5, scale=2)
    private BigDecimal loadIncrementLb;

    @Column(name="max_load_lb", precision=6, scale=2)
    private BigDecimal maxLoadLb;

    protected Machine() {}

    public Machine(String name, BigDecimal loadIncrementLb, BigDecimal maxLoadLb) {
        super(name);
        setLoadIncrementLb(loadIncrementLb);
        setMaxLoadLb(maxLoadLb);
    }

    @Override
    public BigDecimal getLoadIncrementLb() { return loadIncrementLb; }
    public BigDecimal getMaxLoadLb() { return maxLoadLb; }

    public void setLoadIncrementLb(BigDecimal loadIncrementLb) {
        this.loadIncrementLb = requireLoad(loadIncrementLb, MAX_INCREMENT_LB, "Load increment");
    }

    public void setMaxLoadLb(BigDecimal maxLoadLb) {
        this.maxLoadLb = maxLoadLb == null
                ? null
                : requireLoad(maxLoadLb, MAX_STACK_LB, "Max load");
    }

    @Override
    public BigDecimal nextLoad(BigDecimal currentLb) {
        BigDecimal next = super.nextLoad(currentLb);
        return maxLoadLb == null
                ? next
                : next.min(maxLoadLb);
    }

    @Override
    public String progressionHint() {
        String hint = "Add " + loadIncrementLb.stripTrailingZeros().toPlainString() + " lb";
        return maxLoadLb == null
                ? hint
                : hint + " (stack max " + maxLoadLb.stripTrailingZeros().toPlainString() + " lb)";
    }
}

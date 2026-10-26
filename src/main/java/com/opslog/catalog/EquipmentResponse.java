package com.opslog.catalog;

import jakarta.persistence.DiscriminatorValue;
import org.hibernate.Hibernate;

import java.math.BigDecimal;

public record EquipmentResponse(Long id, String name, String kind, BigDecimal loadIncrementLb,
                                BigDecimal maxLoadLb, String progressionHint) {

    static EquipmentResponse from(Equipment item) {
        BigDecimal maxLoad = item instanceof Machine machine ? machine.getMaxLoadLb() : null;
        return new EquipmentResponse(item.getId(), item.getName(), kindOf(item),
                item.getLoadIncrementLb(), maxLoad, item.progressionHint());
    }

    /* reads @DiscriminatorValue so the kind string has one source of truth; getClass() unwraps a lazy proxy */
    static String kindOf(Object entity) {
        return Hibernate.getClass(entity).getAnnotation(DiscriminatorValue.class).value();
    }
}

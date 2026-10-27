package com.opslog.checkin;

import jakarta.persistence.DiscriminatorValue;
import org.hibernate.Hibernate;

public record TierResponse(Long id, String kind, String name, String description, int minSleep, int maxSoreness) {

    static TierResponse from(TrainingTier tier) {
        /* unwraps lazy proxy */
        String kind = Hibernate.getClass(tier).getAnnotation(DiscriminatorValue.class).value();
        return new TierResponse(tier.getId(), kind, tier.getName(), tier.getDescription(),
                tier.getMinSleep(), tier.getMaxSoreness());
    }
}

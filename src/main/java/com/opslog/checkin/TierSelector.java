package com.opslog.checkin;

import com.opslog.common.Require;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;

@Component
public class TierSelector {

    public TrainingTier select(int sleepQuality, int soreness, Collection<? extends TrainingTier> tiers) {
        TrainingTier.requireScore(sleepQuality, "Sleep quality");
        TrainingTier.requireScore(soreness, "Soreness");
        Require.that(tiers != null && !tiers.isEmpty(), "At least one training tier is required");
        return tiers.stream()
                .filter(tier -> tier.accepts(sleepQuality, soreness))
                .max(Comparator.comparingInt(TrainingTier::getIntensity))
                .orElseThrow(() -> new IllegalStateException(
                        "No training tier accepts sleep " + sleepQuality + " and soreness " + soreness
                ));
    }
}

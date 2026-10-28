package com.opslog.logging;

import com.opslog.catalog.Equipment;
import com.opslog.catalog.StrengthExercise;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

/* pure: no repositories, so it is unit tested without mocks */
@Component
public class ProgressionRule {

    /* null = no load to suggest */
    public BigDecimal nextLoad(LastPerformance last) {
        if(!(last.exercise() instanceof StrengthExercise strength)) return null;

        Optional<BigDecimal> working = last.sets().stream()
                .map(SetResult::loadLb)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder());
        if(working.isEmpty()) return null;

        /* only sets at the working load count, so warm-ups do not */
        long hits = last.sets().stream()
                .filter(set -> set.loadLb() != null && set.loadLb().compareTo(working.get()) == 0)
                .filter(set -> set.clean() && set.reps() != null && set.reps() >= strength.getRepMax())
                .count();
        if(hits < last.plannedSets()) return working.get();

        /* smallest jump when more than one item takes load */
        return strength.getEquipment().stream()
                .filter(Equipment::isLoadable)
                .min(Comparator.comparing(Equipment::getLoadIncrementLb).thenComparing(Equipment::getName))
                .map(item -> item.nextLoad(working.get()))
                .orElse(working.get());
    }
}

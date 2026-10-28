package com.opslog.logging;

import com.opslog.catalog.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProgressionRuleTest {

    private final ProgressionRule rule = new ProgressionRule();

    private final MovementPattern push = new MovementPattern("Horizontal Push");
    private final MuscleGroup chest = new MuscleGroup("Chest");
    private final Equipment barbell = new FreeWeight("Barbell", new BigDecimal("5.00"));
    private final Equipment microPlates = new FreeWeight("Micro Plates", new BigDecimal("2.50"));
    private final Equipment bench = new Accessory("Flat Bench");
    private final Equipment cable = new Machine("Cable Stack", new BigDecimal("5.00"), new BigDecimal("200.00"));

    private StrengthExercise strength(int repMin, int repMax, Equipment... items) {
        StrengthExercise exercise = new StrengthExercise("Press", push, chest, null, repMin, repMax);
        for(Equipment item : items) exercise.addEquipment(item);
        return exercise;
    }

    private static SetResult set(int reps, String load) {
        return new SetResult(reps, load == null ? null : new BigDecimal(load), true);
    }

    private static SetResult sloppy(int reps, String load) {
        return new SetResult(reps, new BigDecimal(load), false);
    }

    private BigDecimal next(Exercise exercise, int plannedSets, SetResult... sets) {
        return rule.nextLoad(new LastPerformance(exercise, plannedSets, List.of(sets)));
    }

    @Test
    void everySetAtTheTopOfTheRangeAddsTheIncrement() {
        assertThat(next(strength(5, 8, barbell, bench), 3, set(8, "135"), set(8, "135"), set(8, "135")))
                .isEqualByComparingTo("140");
    }

    @Test
    void oneShortSetKeepsTheLoad() {
        assertThat(next(strength(5, 8, barbell), 3, set(8, "135"), set(8, "135"), set(7, "135")))
                .isEqualByComparingTo("135");
    }

    @Test
    void sloppyTechniqueKeepsTheLoad() {
        assertThat(next(strength(5, 8, barbell), 2, set(8, "135"), sloppy(8, "135")))
                .isEqualByComparingTo("135");
    }

    @Test
    void fewerSetsThanPlannedKeepsTheLoad() {
        assertThat(next(strength(5, 8, barbell), 4, set(8, "135"), set(8, "135"), set(8, "135")))
                .isEqualByComparingTo("135");
    }

    @Test
    void extraSetsDoNotHurt() {
        assertThat(next(strength(5, 8, barbell), 2, set(8, "135"), set(8, "135"), set(5, "135")))
                .isEqualByComparingTo("140");
    }

    @Test
    void lighterSetsDoNotCountTowardTheTarget() {
        assertThat(next(strength(5, 8, barbell), 2, set(8, "95"), set(8, "135"), set(6, "135")))
                .isEqualByComparingTo("135");
    }

    @Test
    void theSmallestIncrementWins() {
        assertThat(next(strength(5, 8, barbell, microPlates), 1, set(8, "135")))
                .isEqualByComparingTo("137.50");
    }

    @Test
    void machineStopsAtTheStackMax() {
        assertThat(next(strength(10, 12, cable), 1, set(12, "200")))
                .isEqualByComparingTo("200");
    }

    @Test
    void bodyweightOnlyKeepsTheLoad() {
        assertThat(next(strength(10, 20, bench), 1, set(20, "25")))
                .isEqualByComparingTo("25");
    }

    @Test
    void noLoadLoggedSuggestsNothing() {
        assertThat(next(strength(5, 8, barbell), 1, set(8, null))).isNull();
        assertThat(next(strength(5, 8, barbell), 1)).isNull();
    }

    @Test
    void timedExercisesDoNotProgressLoad() {
        TimedExercise plank = new TimedExercise("Plank", push, chest, null, 45);
        assertThat(rule.nextLoad(new LastPerformance(plank, 1, List.of(new SetResult(null, new BigDecimal("25"), true)))))
                .isNull();
    }
}

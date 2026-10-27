package com.opslog.checkin;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrainingTierTest {

    private final RecoveryTier recovery = new RecoveryTier("Recovery", "Half the sets at 80%", 1, 1, 5, new BigDecimal("0.80"));
    private final ReducedTier reduced = new ReducedTier("Reduced", "Trimmed accessory work", 2, 3, 3);
    private final FullTier full = new FullTier("Full", "As written", 3, 4, 2);

    @Test
    void fullKeepsTheSessionAsWritten() {
        assertThat(full.scaleSets(4, true)).isEqualTo(4);
        assertThat(full.scaleSets(3, false)).isEqualTo(3);
        assertThat(full.scaleLoad(new BigDecimal("225.00"))).isEqualByComparingTo("225.00");
    }

    @Test
    void reducedTrimsOnlyAccessoryWork() {
        assertThat(reduced.scaleSets(4, true)).isEqualTo(4);
        assertThat(reduced.scaleSets(3, false)).isEqualTo(2);
        assertThat(reduced.scaleSets(1, false)).isEqualTo(1);
        assertThat(reduced.scaleLoad(new BigDecimal("135.00"))).isEqualByComparingTo("135.00");
    }

    @Test
    void recoveryHalvesSetsRoundedUpAndCutsLoad() {
        assertThat(recovery.scaleSets(4, true)).isEqualTo(2);
        assertThat(recovery.scaleSets(3, false)).isEqualTo(2);
        assertThat(recovery.scaleSets(1, true)).isEqualTo(1);
        assertThat(recovery.scaleLoad(new BigDecimal("225.00"))).isEqualByComparingTo("180.00");
        assertThat(recovery.scaleLoad(new BigDecimal("47.50"))).isEqualByComparingTo("38.00");
    }

    @Test
    void unloadedExercisesStayUnloaded() {
        assertThat(recovery.scaleLoad(null)).isNull();
        assertThat(full.scaleLoad(null)).isNull();
    }

    @Test
    void acceptsUsesBothThresholds() {
        assertThat(full.accepts(4, 2)).isTrue();
        assertThat(full.accepts(3, 2)).isFalse();
        assertThat(full.accepts(4, 3)).isFalse();
    }

    @Test
    void rejectsInvalidInput() {
        assertThatThrownBy(() -> full.scaleSets(0, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Planned sets must be greater than zero");
        assertThatThrownBy(() -> full.scaleLoad(new BigDecimal("-5")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Planned load cannot be negative");
        assertThatThrownBy(() -> new FullTier("Full", "As written", 3, 0, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Minimum sleep must be between 1 and 5");
        assertThatThrownBy(() -> new RecoveryTier("Recovery", "Easy", 1, 1, 5, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Load factor must be between 0 and 1");
    }
}

package com.opslog.checkin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/* Documented test 1: tier selection */
class TierSelectorTest {

    /* same as V7 */
    private final RecoveryTier recovery = new RecoveryTier("Recovery", "Half the sets at 80%", 1, 1, 5, new BigDecimal("0.80"));
    private final ReducedTier reduced = new ReducedTier("Reduced", "Trimmed accessory work", 2, 3, 3);
    private final FullTier full = new FullTier("Full", "As written", 3, 4, 2);
    private final List<TrainingTier> tiers = List.of(recovery, reduced, full);

    private final TierSelector selector = new TierSelector();

    @ParameterizedTest(name = "sleep {0}, soreness {1} -> {2}")
    @CsvSource({
            "5, 1, Full",
            "4, 2, Full",
            "3, 1, Reduced",
            "5, 3, Reduced",
            "3, 3, Reduced",
            "2, 1, Recovery",
            "5, 4, Recovery",
            "1, 5, Recovery"
    })
    @DisplayName("picks the most intense tier the check-in qualifies for")
    void selectsTierFromSleepAndSoreness(int sleep, int soreness, String expected) {
        assertThat(selector.select(sleep, soreness, tiers).getName()).isEqualTo(expected);
    }

    @Test
    @DisplayName("every one of the 25 score combinations gets the expected tier")
    void coversEveryCombination() {
        for(int sleep = 1; sleep <= 5; sleep++) {
            for(int soreness = 1; soreness <= 5; soreness++) {
                TrainingTier expected = sleep >= 4 && soreness <= 2 ? full
                        : sleep >= 3 && soreness <= 3 ? reduced
                        : recovery;
                assertThat(selector.select(sleep, soreness, tiers))
                        .as("sleep %d, soreness %d", sleep, soreness)
                        .isSameAs(expected);
            }
        }
    }

    @Test
    @DisplayName("the order the tiers arrive in does not matter")
    void ignoresListOrder() {
        assertThat(selector.select(5, 1, List.of(full, recovery, reduced))).isSameAs(full);
        assertThat(selector.select(3, 3, List.of(full, reduced, recovery))).isSameAs(reduced);
    }

    @Test
    @DisplayName("scores outside 1 to 5 are rejected")
    void rejectsOutOfRangeScores() {
        assertThatThrownBy(() -> selector.select(0, 3, tiers))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Sleep quality must be between 1 and 5");
        assertThatThrownBy(() -> selector.select(3, 6, tiers))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Soreness must be between 1 and 5");
    }

    @Test
    @DisplayName("fails clearly when no tier accepts the scores")
    void failsWhenNoTierMatches() {
        assertThatThrownBy(() -> selector.select(2, 5, List.of(reduced, full)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No training tier accepts sleep 2 and soreness 5");
        assertThatThrownBy(() -> selector.select(3, 3, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("At least one training tier is required");
    }
}

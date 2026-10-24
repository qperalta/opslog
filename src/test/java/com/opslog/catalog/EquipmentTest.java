package com.opslog.catalog;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class EquipmentTest {

    @Test
    void freeWeightRejectsZeroIncrement() {
        assertThrows(IllegalArgumentException.class,
                () -> new FreeWeight("Barbell", BigDecimal.ZERO));
    }

    @Test
    void freeWeightRejectsNegativeIncrement() {
        assertThrows(IllegalArgumentException.class,
                () -> new FreeWeight("Barbell", new BigDecimal("-5")));
    }

    @Test
    void nextLoadRejectsNull() {
        Equipment barbell = new FreeWeight("Barbell", new BigDecimal("5"));

        assertThrows(IllegalArgumentException.class, () -> barbell.nextLoad(null));
    }

    @Test
    void freeWeightAddsIncrement() {
        Equipment barbell = new FreeWeight("Barbell", new BigDecimal("5"));

        assertEquals(0, new BigDecimal("140").compareTo(barbell.nextLoad(new BigDecimal("135"))));
    }

    @Test
    void machineCapsAtStackMax() {
        Equipment legPress = new Machine("Leg Press", new BigDecimal("10"), new BigDecimal("300"));

        assertEquals(0, new BigDecimal("300").compareTo(legPress.nextLoad(new BigDecimal("295"))));
    }

    @Test
    void bodyweightDoesNotAddLoad() {
        Equipment pullUp = new Bodyweight("Pull-up Bar");

        assertFalse(pullUp.isLoadable());
        assertEquals(0, BigDecimal.ZERO.compareTo(pullUp.nextLoad(BigDecimal.ZERO)));
    }
}

package com.opslog.catalog;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ExerciseTest {

    private final MovementPattern squat = new MovementPattern("Squat");
    private final MuscleGroup quads = new MuscleGroup("Quadriceps");

    private StrengthExercise strength(int repMin, int repMax) {
        return new StrengthExercise("Back Squat", squat, quads, null, repMin, repMax);
    }

    @Test
    void strengthRejectsZeroMinReps() {
        assertThrows(IllegalArgumentException.class, () -> strength(0, 8));
    }

    @Test
    void strengthRejectsMaxBelowMin() {
        assertThrows(IllegalArgumentException.class, () -> strength(10, 8));
    }

    @Test
    void strengthRejectsMaxAboveFifty() {
        assertThrows(IllegalArgumentException.class, () -> strength(5, 51));
    }

    @Test
    void strengthDescribesRangeAndSingleRepCount() {
        assertEquals("8-12 reps", strength(8, 12).describeTarget());
        assertEquals("5 reps", strength(5, 5).describeTarget());
    }

    @Test
    void timedDescribesSeconds() {
        Exercise plank = new TimedExercise("Plank", squat, quads, null, 45);

        assertEquals("45 s", plank.describeTarget());
    }

    @Test
    void cardioDescribesMinutesAndKeepsDuration() {
        CardioFinisher bike = new CardioFinisher("Assault Bike", squat, quads, null, 10);

        assertEquals("10 min", bike.describeTarget());
        assertEquals(10, bike.getDurationMinutes());
    }

    @Test
    void catalogExerciseIsVisibleToEveryone() {
        Exercise catalog = strength(8, 12);

        assertFalse(catalog.isCustom());
        assertTrue(catalog.isVisibleTo(1L));
        assertTrue(catalog.isVisibleTo(999L));
    }

    @Test
    void customExerciseIsVisibleOnlyToOwner() {
        Exercise custom = new StrengthExercise("Belt Squat", squat, quads, 1000L, 8, 12);

        assertTrue(custom.isCustom());
        // Long caches only -128..127, so these are two different objects; == would fail, equals passes

        assertTrue(custom.isVisibleTo(Long.valueOf(1000L)));
        assertFalse(custom.isVisibleTo(2000L));
    }

    @Test
    void addAndRemoveEquipment() {
        Exercise squatEx = strength(8, 12);
        Equipment barbell = new FreeWeight("Barbell", new BigDecimal("5"));

        squatEx.addEquipment(barbell);
        assertTrue(squatEx.getEquipment().contains(barbell));

        squatEx.removeEquipment(barbell);
        assertFalse(squatEx.getEquipment().contains(barbell));
    }

    @Test
    void addEquipmentRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> strength(8, 12).addEquipment(null));
    }

    @Test
    void equipmentSetIsUnmodifiable() {
        Exercise squatEx = strength(8, 12);
        Equipment barbell = new FreeWeight("Barbell", new BigDecimal("5"));

        assertThrows(UnsupportedOperationException.class, () -> squatEx.getEquipment().add(barbell));
    }

    @Test
    void missingPrimaryMuscleMessage() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new StrengthExercise("Back Squat", squat, null, null, 8, 12));

        assertEquals("Primary muscle group is required", ex.getMessage());
    }

    @Test
    void movementPatternMessagesNameMovementPattern() {
        IllegalArgumentException blank = assertThrows(IllegalArgumentException.class,
                () -> new MovementPattern(" "));
        IllegalArgumentException tooLong = assertThrows(IllegalArgumentException.class,
                () -> new MovementPattern("x".repeat(41)));

        assertTrue(blank.getMessage().startsWith("Movement pattern"), blank.getMessage());
        assertTrue(tooLong.getMessage().startsWith("Movement pattern"), tooLong.getMessage());
    }

    @Test
    void nameIsTrimmed() {
        Exercise ex = new StrengthExercise("  Back Squat  ", squat, quads, null, 8, 12);

        assertEquals("Back Squat", ex.getName());
    }
}

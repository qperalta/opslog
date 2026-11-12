package com.opslog.session;

import com.opslog.catalog.*;
import com.opslog.checkin.FullTier;
import com.opslog.checkin.TrainingTier;
import com.opslog.location.Location;
import com.opslog.plan.Actor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiSessionWriterTest {

    private final TrainingTier full = new FullTier("Full", "The session as written", 3, 4, 2);

    private long nextId = 1;
    private StrengthExercise backSquat, gobletSquat, pushUp;
    private Location dumbbellGym;

    private <T> T withId(T entity) {
        ReflectionTestUtils.setField(entity, "id", nextId++);
        return entity;
    }

    @BeforeEach
    void setUp() {
        Equipment barbell = withId(new FreeWeight("Barbell", new BigDecimal("5.00")));
        Equipment dumbbell = withId(new FreeWeight("Dumbbell", new BigDecimal("5.00")));
        MovementPattern squat = withId(new MovementPattern("Squat"));
        MovementPattern push = withId(new MovementPattern("Horizontal Push"));
        MuscleGroup quads = withId(new MuscleGroup("Quadriceps"));
        MuscleGroup chest = withId(new MuscleGroup("Chest"));

        backSquat = withId(new StrengthExercise("Back Squat", squat, quads, null, 5, 8));
        backSquat.addEquipment(barbell);
        gobletSquat = withId(new StrengthExercise("Goblet Squat", squat, quads, null, 8, 12));
        gobletSquat.addEquipment(dumbbell);
        pushUp = withId(new StrengthExercise("Push-Up", push, chest, null, 10, 20));

        dumbbellGym = new Location("Dumbbell Gym", null);
        dumbbellGym.replaceEquipment(List.of(dumbbell));
    }

    private SessionInput input(Map<Long, BigDecimal> plannedLoads) {
        return new SessionInput(List.of(new PlannedExercise(gobletSquat, 3, true)), full, dumbbellGym,
                List.of(backSquat, gobletSquat, pushUp), plannedLoads);
    }

    private static ProposedExercise proposed(Long id, Integer sets, String load, String reason) {
        return new ProposedExercise(id, sets, load == null ? null : new BigDecimal(load), reason);
    }

    @Test
    void writesTheProposalAsAnAgentDraft() {
        var writer = new AiSessionWriter(List.of(
                proposed(gobletSquat.getId(), 4, "55", " aim for 10 reps "),
                proposed(pushUp.getId(), 3, null, null)), List.of("Slept well, so one extra squat set", " "));

        SessionDraft draft = writer.generate(input(Map.of()));

        assertThat(writer.source()).isEqualTo(Actor.AGENT);
        assertThat(draft.exercises()).extracting(DraftExercise::exercise).containsExactly(gobletSquat, pushUp);
        DraftExercise squat = draft.exercises().getFirst();
        assertThat(squat.sets()).isEqualTo(4);
        assertThat(squat.targetLoadLb()).isEqualByComparingTo("55");
        assertThat(squat.mainLift()).isTrue();
        assertThat(squat.reason()).isEqualTo("aim for 10 reps");
        assertThat(draft.exercises().get(1).mainLift()).isFalse();
        assertThat(draft.notes()).containsExactly("Written by the AI assistant for the Full tier", "Slept well, so one extra squat set");
    }

    @Test
    void refusesAnExerciseOutsideTheCatalog() {
        var writer = new AiSessionWriter(List.of(proposed(999L, 3, null, null)), null);

        assertThatThrownBy(() -> writer.generate(input(Map.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Exercise 999 is not in the catalog");
    }

    @Test
    void refusesEquipmentTheLocationDoesNotHave() {
        var writer = new AiSessionWriter(List.of(proposed(backSquat.getId(), 3, "185", null)), null);

        assertThatThrownBy(() -> writer.generate(input(Map.of())))
                .hasMessage("Back Squat needs equipment that Dumbbell Gym does not have");
    }

    @Test
    void capsTheLoadJumpAtTenPercentOverTheProgression() {
        Map<Long, BigDecimal> loads = Map.of(gobletSquat.getId(), new BigDecimal("50"));

        assertThat(new AiSessionWriter(List.of(proposed(gobletSquat.getId(), 3, "55", null)), null)
                .generate(input(loads)).exercises().getFirst().targetLoadLb()).isEqualByComparingTo("55");
        assertThatThrownBy(() -> new AiSessionWriter(List.of(proposed(gobletSquat.getId(), 3, "60", null)), null)
                .generate(input(loads)))
                .hasMessage("Goblet Squat: 60 lb is more than 10% above the progression load of 50 lb");
    }

    @Test
    void refusesBadSetsDuplicatesAndAnEmptySession() {
        assertThatThrownBy(() -> new AiSessionWriter(List.of(proposed(pushUp.getId(), 11, null, null)), null)
                .generate(input(Map.of()))).hasMessage("Push-Up: sets must be 1 to 10");
        assertThatThrownBy(() -> new AiSessionWriter(List.of(proposed(pushUp.getId(), 3, null, null),
                proposed(pushUp.getId(), 2, null, null)), null).generate(input(Map.of())))
                .hasMessage("Push-Up is listed twice");
        assertThatThrownBy(() -> new AiSessionWriter(List.of(), null))
                .hasMessage("A session needs at least one exercise");
    }
}

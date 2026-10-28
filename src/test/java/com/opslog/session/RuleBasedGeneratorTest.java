package com.opslog.session;

import com.opslog.catalog.*;
import com.opslog.checkin.FullTier;
import com.opslog.checkin.RecoveryTier;
import com.opslog.checkin.ReducedTier;
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

class RuleBasedGeneratorTest {

    private final RuleBasedGenerator generator = new RuleBasedGenerator();

    private final TrainingTier full = new FullTier("Full", "The session as written", 3, 4, 2);
    private final TrainingTier reduced = new ReducedTier("Reduced", "One fewer set of accessory work", 2, 3, 3);
    private final TrainingTier recovery = new RecoveryTier("Recovery", "Half the sets at 80%", 1, 1, 5, new BigDecimal("0.80"));

    private long nextId = 1;
    private Equipment barbell, dumbbell, flatBench, band;
    private MovementPattern squat, horizontalPush, horizontalPull;
    private MuscleGroup quads, chest, triceps, upperBack, shoulders;
    private StrengthExercise backSquat, gobletSquat, benchPress, dbBenchPress, floorPress, pushUp, closeGripPress,
            barbellRow, bandPullApart, csRow, reverseFly;
    private TimedExercise wallSit;
    private Location fullGym, dumbbellGym, emptyGym;

    private <T> T withId(T entity) {
        ReflectionTestUtils.setField(entity, "id", nextId++);
        return entity;
    }

    private StrengthExercise strength(String name, MovementPattern pattern, MuscleGroup muscle, int repMin, int repMax, Equipment... items) {
        StrengthExercise exercise = withId(new StrengthExercise(name, pattern, muscle, null, repMin, repMax));
        for(Equipment item : items) exercise.addEquipment(item);
        return exercise;
    }

    private Location gym(String name, Equipment... items) {
        Location location = new Location(name, null);
        location.replaceEquipment(List.of(items));
        return location;
    }

    @BeforeEach
    void setUp() {
        barbell = withId(new FreeWeight("Barbell", new BigDecimal("5.00")));
        dumbbell = withId(new FreeWeight("Dumbbell", new BigDecimal("5.00")));
        flatBench = withId(new Accessory("Flat Bench"));
        band = withId(new Accessory("Resistance Band"));

        squat = withId(new MovementPattern("Squat"));
        horizontalPush = withId(new MovementPattern("Horizontal Push"));
        horizontalPull = withId(new MovementPattern("Horizontal Pull"));
        quads = withId(new MuscleGroup("Quadriceps"));
        chest = withId(new MuscleGroup("Chest"));
        triceps = withId(new MuscleGroup("Triceps"));
        upperBack = withId(new MuscleGroup("Upper Back"));
        shoulders = withId(new MuscleGroup("Shoulders"));

        backSquat = strength("Back Squat", squat, quads, 5, 8, barbell);
        gobletSquat = strength("Goblet Squat", squat, quads, 8, 12, dumbbell);
        wallSit = withId(new TimedExercise("Wall Sit", squat, quads, null, 45));
        benchPress = strength("Barbell Bench Press", horizontalPush, chest, 5, 8, barbell, flatBench);
        dbBenchPress = strength("Dumbbell Bench Press", horizontalPush, chest, 8, 12, dumbbell, flatBench);
        floorPress = strength("Dumbbell Floor Press", horizontalPush, chest, 8, 12, dumbbell);
        pushUp = strength("Push-Up", horizontalPush, chest, 10, 20);
        closeGripPress = strength("Close-Grip Floor Press", horizontalPush, triceps, 6, 10, dumbbell);
        barbellRow = strength("Barbell Row", horizontalPull, upperBack, 6, 10, barbell);
        bandPullApart = strength("Band Pull-Apart", horizontalPull, upperBack, 15, 20, band);
        csRow = strength("Chest-Supported Dumbbell Row", horizontalPull, upperBack, 8, 12, dumbbell);
        reverseFly = strength("Reverse Dumbbell Fly", horizontalPull, shoulders, 12, 15, dumbbell);

        fullGym = gym("Downtown", barbell, dumbbell, flatBench, band);
        dumbbellGym = gym("Kapolei", dumbbell, band);
        emptyGym = gym("Hotel", band);
    }

    private List<Exercise> catalog() {
        return List.of(backSquat, gobletSquat, wallSit, benchPress, dbBenchPress, floorPress, pushUp, closeGripPress,
                barbellRow, bandPullApart, csRow, reverseFly);
    }

    private SessionDraft generate(TrainingTier tier, Location location, Map<Long, BigDecimal> loads, PlannedExercise... planned) {
        return generator.generate(new SessionInput(List.of(planned), tier, location, catalog(), loads));
    }

    private SessionDraft generate(TrainingTier tier, Location location, PlannedExercise... planned) {
        return generate(tier, location, Map.of(), planned);
    }

    @Test
    void sourceIsTheRulesEngine() {
        assertThat(generator.source()).isEqualTo(Actor.RULES);
    }

    @Test
    void keepsTheSessionAsWrittenWhenTheLocationHasEverything() {
        SessionDraft draft = generate(full, fullGym, new PlannedExercise(benchPress, 4, true), new PlannedExercise(barbellRow, 3, false));

        assertThat(draft.exercises()).extracting(DraftExercise::exercise).containsExactly(benchPress, barbellRow);
        assertThat(draft.exercises()).extracting(DraftExercise::sets).containsExactly(4, 3);
        assertThat(draft.exercises()).allMatch(e -> e.substitutedFor() == null && e.reason() == null);
        assertThat(draft.notes()).containsExactly("Full tier: The session as written");
    }

    @Test
    void substitutesTheSamePatternAndKindWithEquipmentOnHand() {
        DraftExercise item = generate(full, dumbbellGym, new PlannedExercise(backSquat, 4, true)).exercises().getFirst();

        assertThat(item.exercise()).isEqualTo(gobletSquat);
        assertThat(item.substitutedFor()).isEqualTo(backSquat);
        assertThat(item.reason()).isEqualTo("No Barbell at Kapolei");
    }

    @Test
    void reasonListsEveryMissingItemAndNotesTheSwap() {
        SessionDraft draft = generate(full, emptyGym, new PlannedExercise(benchPress, 3, true));

        assertThat(draft.exercises().getFirst().exercise()).isEqualTo(pushUp);
        assertThat(draft.exercises().getFirst().reason()).isEqualTo("No Barbell, Flat Bench at Hotel");
        assertThat(draft.notes()).contains("Barbell Bench Press -> Push-Up");
    }

    @Test
    void prefersTheSamePrimaryMuscle() {
        DraftExercise item = generate(full, dumbbellGym, new PlannedExercise(benchPress, 3, true)).exercises().getFirst();

        assertThat(item.exercise()).isEqualTo(floorPress);
    }

    @Test
    void prefersTheClosestRepRangeOverTheName() {
        DraftExercise item = generate(full, dumbbellGym, new PlannedExercise(barbellRow, 3, true)).exercises().getFirst();

        assertThat(item.exercise()).isEqualTo(csRow);
    }

    @Test
    void neverSubstitutesAnotherKind() {
        Location noDumbbells = gym("Park", band);
        SessionDraft draft = generate(full, noDumbbells, new PlannedExercise(backSquat, 3, true));

        assertThat(draft.exercises()).isEmpty();
        assertThat(draft.notes()).contains("Back Squat skipped: no substitute at Park");
    }

    @Test
    void neverPicksAnExerciseAlreadyInTheSession() {
        SessionDraft draft = generate(full, dumbbellGym,
                new PlannedExercise(benchPress, 3, true), new PlannedExercise(floorPress, 3, false));

        assertThat(draft.exercises()).extracting(DraftExercise::exercise).containsExactly(pushUp, floorPress);
    }

    @Test
    void twoSwapsNeverPickTheSameSubstitute() {
        SessionDraft draft = generate(full, dumbbellGym,
                new PlannedExercise(benchPress, 3, true), new PlannedExercise(dbBenchPress, 3, false));

        assertThat(draft.exercises()).extracting(DraftExercise::exercise).containsExactly(floorPress, pushUp);
    }

    @Test
    void reducedTierCutsOnlyAccessorySets() {
        SessionDraft draft = generate(reduced, fullGym, new PlannedExercise(benchPress, 4, true), new PlannedExercise(barbellRow, 3, false));

        assertThat(draft.exercises()).extracting(DraftExercise::sets).containsExactly(4, 2);
        assertThat(draft.exercises().get(0).reason()).isNull();
        assertThat(draft.exercises().get(1).reason()).isEqualTo("3 -> 2 sets");
    }

    @Test
    void recoveryTierHalvesSetsAndScalesTheLoadOfTheChosenExercise() {
        SessionDraft draft = generate(recovery, dumbbellGym,
                Map.of(gobletSquat.getId(), new BigDecimal("50.00"), backSquat.getId(), new BigDecimal("225.00")),
                new PlannedExercise(backSquat, 3, true));
        DraftExercise item = draft.exercises().getFirst();

        assertThat(item.sets()).isEqualTo(2);
        assertThat(item.targetLoadLb()).isEqualByComparingTo("40.00");
        assertThat(item.reason()).isEqualTo("No Barbell at Kapolei; 3 -> 2 sets");
    }

    @Test
    void noLoadHistoryLeavesTheTargetLoadEmpty() {
        DraftExercise item = generate(full, fullGym, new PlannedExercise(benchPress, 3, true)).exercises().getFirst();

        assertThat(item.targetLoadLb()).isNull();
    }

    @Test
    void mainLiftFlagCarriesOverToTheSubstitute() {
        DraftExercise item = generate(full, dumbbellGym, new PlannedExercise(backSquat, 3, true)).exercises().getFirst();

        assertThat(item.mainLift()).isTrue();
    }
}

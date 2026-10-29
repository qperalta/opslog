package com.opslog.report;

import com.opslog.catalog.LookupResponse;
import com.opslog.logging.SetResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/* pure arithmetic over plain records, so no mocks and no database */
class ReportCalculatorTest {

    private static final long CHEST = 1, BACK = 2, LEGS = 3;
    private static final List<LookupResponse> MUSCLES = List.of(
            new LookupResponse(BACK, "Upper Back"), new LookupResponse(CHEST, "Chest"),
            new LookupResponse(LEGS, "Quadriceps"));
    private static final LocalDate MON = LocalDate.of(2026, 10, 26), TUE = MON.plusDays(1), WED = MON.plusDays(2);

    private final ReportCalculator calculator = new ReportCalculator();

    private static SetResult set(Integer reps, String load, boolean clean) {
        return new SetResult(reps, load == null ? null : new BigDecimal(load), clean);
    }

    private static ExerciseFacts exercise(long muscle, Direction direction, int planned, SetResult... sets) {
        return new ExerciseFacts(muscle, direction, planned, List.of(sets));
    }

    private static SessionFacts session(LocalDate date, ExerciseFacts... exercises) {
        return new SessionFacts(date, "Upper A", "Full", "Kaimana Downtown", List.of(exercises));
    }

    @Test
    void trainingRowCountsSetsCleanAndVolume() {
        SessionFacts monday = session(MON,
                exercise(CHEST, Direction.PUSH, 4, set(8, "135", true), set(8, "135", true), set(6, "135", false)),
                exercise(BACK, Direction.PULL, 4, set(10, "95", true)),
                exercise(LEGS, null, 3));

        TrainingReportRow row = calculator.trainingRow(monday);

        assertThat(row.date()).isEqualTo(MON);
        assertThat(row.workout()).isEqualTo("Upper A");
        assertThat(row.exercisesDone()).isEqualTo(2);
        assertThat(row.exercisesPlanned()).isEqualTo(3);
        assertThat(row.setsLogged()).isEqualTo(4);
        assertThat(row.cleanPercent()).isEqualTo(75);
        // 2 x 8 x 135 + 6 x 135 + 10 x 95
        assertThat(row.volumeLb()).isEqualByComparingTo("3920");
    }

    @Test
    void setsWithoutRepsOrLoadAddNoVolume() {
        SessionFacts monday = session(MON,
                exercise(CHEST, Direction.PUSH, 3, set(12, null, true)),
                exercise(LEGS, null, 1, set(null, null, true)));

        TrainingReportRow row = calculator.trainingRow(monday);

        assertThat(row.setsLogged()).isEqualTo(2);
        assertThat(row.volumeLb()).isEqualByComparingTo("0");
    }

    @Test
    void aSessionWithNothingLoggedHasNoCleanPercent() {
        TrainingReportRow row = calculator.trainingRow(session(MON, exercise(CHEST, Direction.PUSH, 4)));

        assertThat(row.exercisesDone()).isZero();
        assertThat(row.setsLogged()).isZero();
        assertThat(row.cleanPercent()).isNull();
        assertThat(row.volumeLb()).isEqualByComparingTo("0");
    }

    @Test
    void muscleBalanceListsEveryMuscleMostSetsFirst() {
        List<SessionFacts> sessions = List.of(
                session(MON, exercise(CHEST, Direction.PUSH, 4, set(8, "135", true), set(8, "135", true))),
                session(WED, exercise(CHEST, Direction.PUSH, 4, set(8, "140", true)),
                        exercise(BACK, Direction.PULL, 4, set(10, "95", true))));

        List<MuscleBalanceRow> rows = calculator.muscleBalance(MUSCLES, sessions);

        assertThat(rows).extracting(MuscleBalanceRow::muscle).containsExactly("Chest", "Upper Back", "Quadriceps");
        assertThat(rows.get(0)).isEqualTo(new MuscleBalanceRow("Chest", 3, 75, 2, WED));
        assertThat(rows.get(1)).isEqualTo(new MuscleBalanceRow("Upper Back", 1, 25, 1, WED));
        assertThat(rows.get(2)).isEqualTo(new MuscleBalanceRow("Quadriceps", 0, 0, 0, null));
    }

    @Test
    void plannedButUnloggedExercisesDoNotCountAsTrained() {
        List<SessionFacts> sessions = List.of(
                session(MON, exercise(BACK, Direction.PULL, 4, set(10, "95", true))),
                session(TUE, exercise(BACK, Direction.PULL, 4)));

        MuscleBalanceRow back = calculator.muscleBalance(MUSCLES, sessions).getFirst();

        assertThat(back.sessions()).isEqualTo(1);
        assertThat(back.lastTrained()).isEqualTo(MON);
    }

    @Test
    void tiesOnSetsAreOrderedByName() {
        List<MuscleBalanceRow> rows = calculator.muscleBalance(MUSCLES, List.of());

        assertThat(rows).extracting(MuscleBalanceRow::muscle).containsExactly("Chest", "Quadriceps", "Upper Back");
        assertThat(rows).allSatisfy(row -> assertThat(row.sharePercent()).isZero());
    }

    @Test
    void directionBalanceSharesArePushPlusPullOnly() {
        List<SessionFacts> sessions = List.of(session(MON,
                exercise(CHEST, Direction.PUSH, 4, set(8, "135", true), set(8, "135", true), set(8, "135", true)),
                exercise(BACK, Direction.PULL, 4, set(10, "95", true)),
                exercise(LEGS, null, 3, set(5, "225", true), set(5, "225", true))));

        List<DirectionBalanceRow> rows = calculator.directionBalance(sessions);

        assertThat(rows).containsExactly(
                new DirectionBalanceRow("Push", 3, 75, 1),
                new DirectionBalanceRow("Pull", 1, 25, 1));
    }

    @Test
    void pushPullRatioIsRoundedToTwoPlaces() {
        List<SessionFacts> sessions = List.of(session(MON,
                exercise(CHEST, Direction.PUSH, 4, set(8, "135", true), set(8, "135", true)),
                exercise(BACK, Direction.PULL, 4, set(10, "95", true), set(10, "95", true), set(10, "95", true))));

        assertThat(calculator.pushPullRatio(sessions)).isEqualByComparingTo("0.67");
    }

    @Test
    void noPullSetsMeansNoRatio() {
        List<SessionFacts> sessions = List.of(session(MON, exercise(CHEST, Direction.PUSH, 4, set(8, "135", true))));

        assertThat(calculator.pushPullRatio(sessions)).isNull();
        assertThat(calculator.directionBalance(sessions)).containsExactly(
                new DirectionBalanceRow("Push", 1, 100, 1),
                new DirectionBalanceRow("Pull", 0, 0, 0));
    }
}

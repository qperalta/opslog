package com.opslog.plan;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static java.time.DayOfWeek.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WeekPlannerTest {

    /* a Monday */
    private static final LocalDate WEEK_START = LocalDate.of(2026, 11, 2);

    private final WeekPlanner planner = new WeekPlanner();

    private ProgramTemplate upperLower;
    private TemplateDay upperA, lowerA, upperB, lowerB;
    private List<PlanDay> week;

    @BeforeEach
    void setUp() {
        upperLower = new ProgramTemplate("Upper/Lower", "Four days", List.of(MONDAY, TUESDAY, THURSDAY, FRIDAY));
        upperA = upperLower.addDay("Upper A");
        lowerA = upperLower.addDay("Lower A");
        upperB = upperLower.addDay("Upper B");
        lowerB = upperLower.addDay("Lower B");
        week = new ArrayList<>();
        for(int i = 0; i < 7; i++) week.add(new PlanDay(1L, WEEK_START.plusDays(i)));
    }

    private PlanDay day(DayOfWeek dayOfWeek) { return week.get(dayOfWeek.getValue() - 1); }

    private List<String> workouts() {
        return week.stream().map(d -> d.isTraining() ? d.getWorkout().getName() : "rest").toList();
    }

    @Test
    void autoWeekTrainsOnTheTemplateDaysInRotationOrder() {
        planner.plan(week, upperLower, null);
        assertThat(workouts()).containsExactly("Upper A", "Lower A", "rest", "Upper B", "Lower B", "rest", "rest");
        assertThat(week).allMatch(d -> d.getUpdatedBy() == Actor.RULES);
    }

    @Test
    void rotationContinuesFromThePreviousWorkout() {
        planner.plan(week, upperLower, lowerA);
        assertThat(workouts()).containsExactly("Upper B", "Lower B", "rest", "Upper A", "Lower A", "rest", "rest");
    }

    @Test
    void rotationWrapsAfterTheLastWorkout() {
        planner.plan(week, upperLower, lowerB);
        assertThat(day(MONDAY).getWorkout()).isEqualTo(upperA);
    }

    @Test
    void restDayShiftsTheRotation() {
        day(MONDAY).edit(PlanMode.REST, null, null, false, Actor.USER);
        planner.plan(week, upperLower, null);
        assertThat(workouts()).containsExactly("rest", "Upper A", "rest", "Lower A", "Upper B", "rest", "rest");
    }

    @Test
    void trainingDayWithoutAWorkoutGetsTheNextOne() {
        day(WEDNESDAY).edit(PlanMode.TRAINING, null, null, false, Actor.USER);
        planner.plan(week, upperLower, null);
        assertThat(workouts()).containsExactly("Upper A", "Lower A", "Upper B", "Lower B", "Upper A", "rest", "rest");
    }

    @Test
    void chosenWorkoutIsKeptAndTheRotationContinuesAfterIt() {
        day(MONDAY).edit(PlanMode.TRAINING, upperB, null, false, Actor.USER);
        planner.plan(week, upperLower, null);
        assertThat(workouts()).containsExactly("Upper B", "Lower B", "rest", "Upper A", "Lower A", "rest", "rest");
        assertThat(day(MONDAY).getUpdatedBy()).isEqualTo(Actor.USER);
    }

    @Test
    void lockedDaysAreNotTouchedButStillCountInTheRotation() {
        day(WEDNESDAY).edit(PlanMode.AUTO, upperA, null, true, Actor.USER);
        day(MONDAY).edit(PlanMode.AUTO, null, null, true, Actor.USER);
        planner.plan(week, upperLower, null);
        assertThat(workouts()).containsExactly("rest", "Upper A", "Upper A", "Lower A", "Upper B", "rest", "rest");
        assertThat(day(WEDNESDAY).getUpdatedBy()).isEqualTo(Actor.USER);
    }

    @Test
    void lockedTrainingDayOnNextInRotationGetsItsWorkoutOnce() {
        day(SATURDAY).edit(PlanMode.TRAINING, null, null, true, Actor.USER);
        planner.plan(week, upperLower, null);
        assertThat(workouts()).containsExactly("Upper A", "Lower A", "rest", "Upper B", "Lower B", "Upper A", "rest");
        assertThat(day(SATURDAY).isLocked()).isTrue();
        assertThat(day(SATURDAY).getUpdatedBy()).isEqualTo(Actor.USER);

        // once filled it is locked like any other day: a rest day earlier in the week no longer moves it
        day(MONDAY).edit(PlanMode.REST, null, null, false, Actor.USER);
        planner.plan(week, upperLower, null);
        assertThat(workouts()).containsExactly("rest", "Upper A", "rest", "Lower A", "Upper B", "Upper A", "rest");
    }

    @Test
    void workoutFromAnotherTemplateRestartsTheRotation() {
        ProgramTemplate fullBody = new ProgramTemplate("Full Body", "Three days", List.of(MONDAY));
        TemplateDay fullBodyA = fullBody.addDay("Full Body A");
        day(TUESDAY).edit(PlanMode.TRAINING, fullBodyA, null, false, Actor.USER);
        planner.plan(week, upperLower, fullBodyA);
        assertThat(workouts()).containsExactly("Upper A", "Lower A", "rest", "Upper B", "Lower B", "rest", "rest");
    }

    @Test
    void replanningAnUnchangedWeekKeepsTheSameWorkouts() {
        planner.plan(week, upperLower, null);
        List<String> first = workouts();
        planner.plan(week, upperLower, null);
        assertThat(workouts()).isEqualTo(first);
    }

    @Test
    void templateWithoutWorkoutsIsRejected() {
        ProgramTemplate empty = new ProgramTemplate("Empty", "No workouts", List.of(MONDAY));
        assertThatThrownBy(() -> planner.plan(week, empty, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The program has no workouts");
    }
}

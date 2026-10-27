package com.opslog.plan;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlanDayTest {

    private final ProgramTemplate template = new ProgramTemplate("Upper/Lower", "Four days", List.of(DayOfWeek.MONDAY));
    private final TemplateDay upperA = template.addDay("Upper A");

    @Test
    void newDayIsAutoAndUnplanned() {
        PlanDay day = new PlanDay(1L, LocalDate.of(2026, 11, 2));
        assertThat(day.getMode()).isEqualTo(PlanMode.AUTO);
        assertThat(day.isTraining()).isFalse();
        assertThat(day.isLocked()).isFalse();
        assertThat(day.getUpdatedBy()).isEqualTo(Actor.RULES);
    }

    @Test
    void restDayCannotHaveAWorkout() {
        PlanDay day = new PlanDay(1L, LocalDate.of(2026, 11, 2));
        assertThatThrownBy(() -> day.edit(PlanMode.REST, upperA, null, false, Actor.USER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A rest day cannot have a workout");
    }

    @Test
    void rulesEngineCannotEditOrRescheduleALockedDay() {
        PlanDay day = new PlanDay(1L, LocalDate.of(2026, 11, 2));
        assertThatThrownBy(() -> day.edit(PlanMode.AUTO, null, null, false, Actor.RULES))
                .isInstanceOf(IllegalArgumentException.class);
        day.edit(PlanMode.TRAINING, upperA, null, true, Actor.USER);
        assertThatThrownBy(() -> day.schedule(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A locked day cannot be re-planned");
        assertThat(day.getWorkout()).isEqualTo(upperA);
    }

    @Test
    void editRecordsTheActor() {
        PlanDay day = new PlanDay(1L, LocalDate.of(2026, 11, 2));
        day.edit(PlanMode.TRAINING, upperA, null, false, Actor.AGENT);
        assertThat(day.getUpdatedBy()).isEqualTo(Actor.AGENT);
        assertThat(day.isTraining()).isTrue();
    }
}

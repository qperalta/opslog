package com.opslog.plan;

import com.opslog.location.LocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/* documented test 2: the locked-day guard */
@ExtendWith(MockitoExtension.class)
class PlanDayServiceTest {

    private static final long USER_ID = 1L;
    private static final LocalDate DATE = LocalDate.of(2026, 11, 2);

    @Mock PlanDayRepository days;
    @Mock ProgramService programService;
    @Mock LocationService locationService;
    @Mock PlanService planService;

    private PlanDayService service;
    private TemplateDay upperA;
    private PlanDay day;

    @BeforeEach
    void setUp() {
        service = new PlanDayService(days, programService, locationService, planService);
        ProgramTemplate template = new ProgramTemplate("Upper/Lower", "Four days", List.of(DayOfWeek.MONDAY));
        upperA = template.addDay("Upper A");
        UserProgram program = new UserProgram(USER_ID);
        program.choose(template, null, DATE);
        lenient().when(programService.find(USER_ID)).thenReturn(program);
        day = new PlanDay(USER_ID, DATE);
        when(days.findByUserIdAndDate(USER_ID, DATE)).thenReturn(Optional.of(day));
    }

    private void lockAsUser() { day.edit(PlanMode.AUTO, upperA, null, true, Actor.USER); }

    @Test
    void agentCannotChangeALockedDay() {
        lockAsUser();
        PlanDayRequest rest = new PlanDayRequest(PlanMode.REST, null, null, false);

        assertThatThrownBy(() -> service.setDay(USER_ID, DATE, rest, Actor.AGENT))
                .isInstanceOf(LockedDayException.class)
                .hasMessage("The plan for 2026-11-02 is locked");

        assertThat(day.getWorkout()).isEqualTo(upperA);
        assertThat(day.isLocked()).isTrue();
        assertThat(day.getUpdatedBy()).isEqualTo(Actor.USER);
        verify(days, never()).saveAndFlush(any());
        verifyNoInteractions(programService, locationService, planService);
    }

    @Test
    void agentCanChangeAnUnlockedDay() {
        PlanDayResponse response = service.setDay(USER_ID, DATE, new PlanDayRequest(PlanMode.REST, null, null, false), Actor.AGENT);

        assertThat(response.mode()).isEqualTo(PlanMode.REST);
        assertThat(response.updatedBy()).isEqualTo(Actor.AGENT);
        verify(days).saveAndFlush(day);
        verify(planService).replanFrom(USER_ID, DATE);
    }

    @Test
    void agentCannotLockADay() {
        service.setDay(USER_ID, DATE, new PlanDayRequest(PlanMode.AUTO, null, null, true), Actor.AGENT);

        assertThat(day.isLocked()).isFalse();
    }

    @Test
    void userCanChangeTheirOwnLockedDay() {
        lockAsUser();

        PlanDayResponse response = service.setDay(USER_ID, DATE, new PlanDayRequest(PlanMode.REST, null, null, false), Actor.USER);

        assertThat(response.mode()).isEqualTo(PlanMode.REST);
        assertThat(response.workout()).isNull();
        assertThat(response.locked()).isFalse();
        assertThat(response.updatedBy()).isEqualTo(Actor.USER);
    }

    @Test
    void lockingAnAutoDayKeepsTheWorkoutTheRulesPicked() {
        day.schedule(upperA);

        service.setDay(USER_ID, DATE, new PlanDayRequest(PlanMode.AUTO, null, null, true), Actor.USER);

        assertThat(day.getWorkout()).isEqualTo(upperA);
        assertThat(day.isLocked()).isTrue();
    }
}

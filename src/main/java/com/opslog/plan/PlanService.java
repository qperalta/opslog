package com.opslog.plan;

import com.opslog.common.Require;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class PlanService {

    private static final long MAX_WEEKS_AWAY = 52;

    private final PlanDayRepository days;
    private final ProgramService programService;
    private final WeekPlanner planner;

    PlanService(PlanDayRepository days, ProgramService programService, WeekPlanner planner) {
        this.days = days;
        this.programService = programService;
        this.planner = planner;
    }

    /* Monday to Sunday; missing days are created, today onward is planned */
    public WeekResponse week(long userId, LocalDate anyDay) {
        LocalDate start = anyDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Require.that(Math.abs(ChronoUnit.WEEKS.between(planningStart(), start)) <= MAX_WEEKS_AWAY,
                "The week must be within a year of today");
        UserProgram program = programService.find(userId);

        Map<LocalDate, PlanDay> stored = days.findByUserIdAndDateBetweenOrderByDate(userId, start, start.plusDays(6))
                .stream().collect(Collectors.toMap(PlanDay::getDate, Function.identity()));
        List<PlanDay> week = start.datesUntil(start.plusDays(7))
                .map(date -> stored.computeIfAbsent(date, d -> new PlanDay(userId, d)))
                .toList();

        plan(userId, program.getTemplate(), week.stream().filter(day -> !day.getDate().isBefore(planningStart())).toList());
        days.saveAll(week);
        return WeekResponse.from(start, program, week);
    }

    /* after an edit or a program switch; past days are history */
    public void replanFrom(long userId, LocalDate from) {
        LocalDate start = from.isBefore(planningStart()) ? planningStart() : from;
        UserProgram program = programService.find(userId);
        plan(userId, program.getTemplate(), days.findByUserIdAndDateGreaterThanEqualOrderByDate(userId, start));
    }

    private void plan(long userId, ProgramTemplate template, List<PlanDay> toPlan) {
        if(toPlan.isEmpty()) return;
        TemplateDay previous = days.findFirstByUserIdAndDateBeforeAndWorkoutIsNotNullOrderByDateDesc(userId, toPlan.getFirst().getDate())
                .map(PlanDay::getWorkout)
                .orElse(null);
        planner.plan(toPlan, template, previous);
    }

    /* -1 day for time zones behind UTC */
    private static LocalDate planningStart() { return LocalDate.now(ZoneOffset.UTC).minusDays(1); }
}

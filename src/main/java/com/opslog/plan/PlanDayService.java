package com.opslog.plan;

import com.opslog.location.Location;
import com.opslog.location.LocationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional
public class PlanDayService {

    private final PlanDayRepository days;
    private final ProgramService programService;
    private final LocationService locationService;
    private final PlanService planService;

    PlanDayService(PlanDayRepository days, ProgramService programService, LocationService locationService, PlanService planService) {
        this.days = days;
        this.programService = programService;
        this.locationService = locationService;
        this.planService = planService;
    }

    public PlanDayResponse setDay(long userId, LocalDate date, PlanDayRequest request, Actor actor) {
        PlanDay day = days.findByUserIdAndDate(userId, date).orElseGet(() -> new PlanDay(userId, date));
        // lock:
        if(actor == Actor.AGENT && day.isLocked()) throw new LockedDayException(date);

        UserProgram program = programService.find(userId);
        TemplateDay workout = switch(request.mode()) {
            case REST -> null;
            case TRAINING -> findWorkout(program, request.workoutId());
            case AUTO -> day.getWorkout();
        };
        Location location = request.locationId() == null
                ? null
                : locationService.findVisible(userId, request.locationId());
        boolean locked = actor == Actor.USER // only user can lock/unlock
                ? request.locked()
                : day.isLocked();

        day.edit(request.mode(), workout, location, locked, actor);
        days.saveAndFlush(day);
        planService.replanFrom(userId, date);
        return PlanDayResponse.from(day);
    }

    private TemplateDay findWorkout(UserProgram program, Long workoutId) {
        if(workoutId == null) return null;
        return program.getTemplate().getDays().stream()
                .filter(workout -> workout.getId().equals(workoutId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Workout " + workoutId + " is not part of your program"));
    }
}

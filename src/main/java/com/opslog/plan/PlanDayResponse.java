package com.opslog.plan;

import com.opslog.catalog.LookupResponse;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;

public record PlanDayResponse(LocalDate date, DayOfWeek dayOfWeek, PlanMode mode, LookupResponse workout,
                              LookupResponse location, boolean locked, Actor updatedBy, Instant updatedAt) {

    static PlanDayResponse from(PlanDay day) {
        LookupResponse workout = day.getWorkout() == null
                ? null
                : new LookupResponse(day.getWorkout().getId(), day.getWorkout().getName());
        LookupResponse location = day.getLocation() == null
                ? null
                : new LookupResponse(day.getLocation().getId(), day.getLocation().getName());
        return new PlanDayResponse(day.getDate(), day.getDate().getDayOfWeek(), day.getMode(), workout, location,
                day.isLocked(), day.getUpdatedBy(), day.getUpdatedAt());
    }
}

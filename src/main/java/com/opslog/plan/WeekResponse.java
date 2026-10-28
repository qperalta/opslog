package com.opslog.plan;

import java.time.LocalDate;
import java.util.List;

public record WeekResponse(LocalDate start, LocalDate end, UserProgramResponse program, List<PlanDayResponse> days) {

    static WeekResponse from(LocalDate start, UserProgram program, List<PlanDay> days) {
        return new WeekResponse(start, start.plusDays(6), UserProgramResponse.from(program),
                days.stream().map(PlanDayResponse::from).toList());
    }
}

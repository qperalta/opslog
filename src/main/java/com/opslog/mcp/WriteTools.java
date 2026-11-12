package com.opslog.mcp;

import com.opslog.catalog.LookupResponse;
import com.opslog.common.Require;
import com.opslog.location.LocationService;
import com.opslog.plan.Actor;
import com.opslog.plan.PlanDayRequest;
import com.opslog.plan.PlanDayResponse;
import com.opslog.plan.PlanDayService;
import com.opslog.plan.PlanMode;
import com.opslog.plan.ProgramService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static com.opslog.mcp.ToolArgs.*;

/* READ_WRITE tools; every write goes through the service as Actor.AGENT, so locked days stay read-only */
@Component
public class WriteTools {

    private final McpCaller caller;
    private final PlanDayService planDays;
    private final ProgramService programs;
    private final LocationService locations;

    WriteTools(McpCaller caller, PlanDayService planDays, ProgramService programs, LocationService locations) {
        this.caller = caller;
        this.planDays = planDays;
        this.programs = programs;
        this.locations = locations;
    }

    @McpTool(name="set_plan_day", annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = true, openWorldHint = false),
            description = "Set one day of the member's plan: TRAINING with a workout from their program, REST, or AUTO to let the rules engine pick. " +
                    "Days the member locked cannot be changed, and the agent cannot lock or unlock a day. Needs a read and write token")
    public PlanDayResponse setPlanDay(
            @McpToolParam(description = "Day as YYYY-MM-DD") String date,
            @McpToolParam(description = "AUTO, TRAINING or REST") String mode,
            @McpToolParam(description = "Workout name from the member's program, only for TRAINING. Leave out to let the rules engine pick",
                    required = false) String workout,
            @McpToolParam(description = "Location name, e.g. Kaimana Downtown. Leave out for the program's location",
                    required = false) String location) {
        String detail = "date=" + date + " mode=" + mode + " workout=" + workout + " location=" + location;
        return caller.call("set_plan_day", McpScope.READ_WRITE, detail, principal -> {
            Require.that(date != null && !date.isBlank(), "Date is required");
            PlanMode planMode = modeOf(mode);
            Require.that(planMode == PlanMode.TRAINING || workout == null || workout.isBlank(), "A workout can only be set for TRAINING");
            LocalDate day = dateOr(date, null);
            long userId = principal.userId();
            Long workoutId = planMode == PlanMode.TRAINING ? idOf("workout", workout, workouts(userId)) : null;
            Long locationId = idOf("location", location, locations.list(userId).stream()
                    .map(item -> new LookupResponse(item.id(), item.name())).toList());
            // locked is ignored for AGENT; the service keeps the member's lock
            return planDays.setDay(userId, day, new PlanDayRequest(planMode, workoutId, locationId, false), Actor.AGENT);
        });
    }

    /* the workouts in the member's chosen program */
    private List<LookupResponse> workouts(long userId) {
        long templateId = programs.get(userId).template().id();
        return programs.templates().stream()
                .filter(template -> template.id() == templateId)
                .flatMap(template -> template.workouts().stream())
                .map(item -> new LookupResponse(item.id(), item.name()))
                .toList();
    }

    private static PlanMode modeOf(String text) {
        Require.that(text != null && !text.isBlank(), "Mode is required");
        try {
            return PlanMode.valueOf(text.strip().toUpperCase());
        } catch(IllegalArgumentException ex) {
            throw new IllegalArgumentException("Mode must be one of " + Arrays.toString(PlanMode.values()) + ", got " + text.strip());
        }
    }
}

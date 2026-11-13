package com.opslog.mcp;

import com.opslog.catalog.CatalogService;
import com.opslog.catalog.ExerciseResponse;
import com.opslog.catalog.ExerciseSearch;
import com.opslog.catalog.LookupResponse;
import com.opslog.checkin.CheckInResponse;
import com.opslog.checkin.CheckInService;
import com.opslog.common.PageResponse;
import com.opslog.common.Require;
import com.opslog.logging.HistoryRow;
import com.opslog.logging.HistorySearch;
import com.opslog.logging.HistoryService;
import com.opslog.plan.PlanService;
import com.opslog.plan.WeekResponse;
import com.opslog.report.ReportSearch;
import com.opslog.report.ReportService;
import com.opslog.report.TrainingReport;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.context.McpSyncRequestContext;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.opslog.mcp.ToolArgs.*;

@Component
public class ReadTools {

    private final McpCaller caller;
    private final CheckInService checkIns;
    private final HistoryService history;
    private final PlanService plans;
    private final CatalogService catalog;
    private final ReportService reports;

    ReadTools(McpCaller caller, CheckInService checkIns, HistoryService history, PlanService plans,
              CatalogService catalog, ReportService reports) {
        this.caller = caller;
        this.checkIns = checkIns;
        this.history = history;
        this.plans = plans;
        this.catalog = catalog;
        this.reports = reports;
    }

    @McpTool(name = "get_checkin", annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false),
            description = "The morning check-in for one day: sleep quality and soreness (1 to 5), the location, and the training tier it picked")
    public CheckInResponse getCheckin(McpSyncRequestContext context, @McpToolParam(description = "Day as YYYY-MM-DD. Leave out for today (UTC)",
    required = false) String date) {
        return caller.call(context, "get_checkin", McpScope.READ, "date=" + date, principal ->
                checkIns.get(principal.userId(), dateOr(date, today())));
    }

    @McpTool(name="list_logs", annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false),
            description = "Logged sets, newest day first: date, workout, exercise, set number, reps or seconds, load in lb, and whether the set was clean")
    public PageResponse<HistoryRow> listLogs(McpSyncRequestContext context,
            @McpToolParam(description = "First day as YYYY-MM-DD", required = false) String from,
            @McpToolParam(description = "Last day as YYYY-MM-DD", required = false) String to,
            @McpToolParam(description = "Only this exercise; ids come from search_exercises", required = false) Long exerciseId,
            @McpToolParam(description = "Page number from 0", required = false) Integer page,
            @McpToolParam(description = "Rows per page, 1 to 100. Default 20", required = false) Integer size) {
        String detail = "from=" + from + " to=" + to + " exerciseId=" + exerciseId + " page=" + page;
        return caller.call(context, "list_logs", McpScope.READ, detail, principal -> {
            // same limits as HistorySearch
            Require.that(page == null || page >= 0, "Page must be 0 or more");
            Require.that(size == null || (size >= 1 && size <= 100), "Page size must be 1 to 100");
            var search = new HistorySearch(exerciseId, null, dateOr(from, null), dateOr(to, null), page, size);
            return history.search(principal.userId(), search);
        });
    }

    @McpTool(name="get_plan", annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false),
            description = "The plan for one week, Monday to Sunday: each day's mode, workout, location, and whether the member locked it")
    public WeekResponse getPlan(McpSyncRequestContext context, @McpToolParam(description = "Any day in the week as YYYY-MM-DD. Leave out for this week",
            required = false) String week) {
        return caller.call(context, "get_plan", McpScope.READ, "week=" + week, principal ->
                plans.week(principal.userId(), dateOr(week, today())));
    }

    @McpTool(name="search_exercises", annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false),
            description = "Exercises from the catalog and the member's own, sorted by name: id, kind, movement pattern, primary muscle, " +
                    "target reps or time, and equipment. Filters are optional and combine; an unknown name lists the valid ones")
    public PageResponse<ExerciseResponse> searchExercises(McpSyncRequestContext context,
            @McpToolParam(description = "Part of the exercise name", required = false) String query,
            @McpToolParam(description = "Primary muscle group by name, e.g. Quadriceps", required = false) String muscleGroup,
            @McpToolParam(description = "Movement pattern by name, e.g. Horizontal Pull", required = false) String movementPattern,
            @McpToolParam(description = "Equipment by name, e.g. Barbell", required = false) String equipment,
            @McpToolParam(description = "STRENGTH, TIMED or CARDIO", required = false) String kind,
            @McpToolParam(description = "Page number from 0", required = false) Integer page,
            @McpToolParam(description = "Rows per page, 1 to 100. Default 20", required = false) Integer size) {
        String detail = "query=" + query + " muscleGroup=" + muscleGroup + " movementPattern=" + movementPattern
                + " equipment=" + equipment + " kind=" + kind + " page=" + page;
        return caller.call(context, "search_exercises", McpScope.READ, detail, principal -> {
            // same limits as ExerciseSearch
            Require.that(query == null || query.strip().length() <= 80, "Search text must be 80 characters or fewer");
            Require.that(page == null || page >= 0, "Page must be 0 or more");
            Require.that(size == null || (size >= 1 && size <= 100), "Page size must be 1 to 100");
            List<LookupResponse> equipmentNames = catalog.equipment().stream()
                    .map(item -> new LookupResponse(item.id(), item.name())).toList();
            var search = new ExerciseSearch(query,
                    idOf("muscle group", muscleGroup, catalog.muscleGroups()),
                    idOf("movement pattern", movementPattern, catalog.movementPatterns()),
                    idOf("equipment", equipment, equipmentNames),
                    kind, page, size);
            return catalog.searchExercises(principal.userId(), search);
        });
    }

    @McpTool(name="get_training_report", annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false),
            description = "The Training Report for a date range, one row per session: date, workout, tier, location, exercises done of planned, " +
                    "sets logged, percent of clean sets, and volume in lb, plus totals. Leave out both dates for the last 28 days")
    public TrainingReport getTrainingReport(McpSyncRequestContext context,
            @McpToolParam(description = "First day as YYYY-MM-DD", required = false) String from,
            @McpToolParam(description = "Last day as YYYY-MM-DD. Default today (UTC)", required = false) String to) {
        return caller.call(context, "get_training_report", McpScope.READ, "from=" + from + " to=" + to, principal ->
                reports.training(principal.userId(), new ReportSearch(dateOr(from, null), dateOr(to, null))));
    }
}

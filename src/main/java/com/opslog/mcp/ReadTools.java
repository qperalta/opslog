package com.opslog.mcp;

import com.opslog.checkin.CheckInResponse;
import com.opslog.checkin.CheckInService;
import com.opslog.common.PageResponse;
import com.opslog.common.Require;
import com.opslog.logging.HistoryRow;
import com.opslog.logging.HistorySearch;
import com.opslog.logging.HistoryService;
import com.opslog.plan.PlanService;
import com.opslog.plan.WeekResponse;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

@Component
public class ReadTools {

    private final McpCaller caller;
    private final CheckInService checkIns;
    private final HistoryService history;
    private final PlanService plans;

    ReadTools(McpCaller caller, CheckInService checkIns, HistoryService history, PlanService plans) {
        this.caller = caller;
        this.checkIns = checkIns;
        this.history = history;
        this.plans = plans;
    }

    @McpTool(name = "get_checkin", annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false),
            description = "The morning check-in for one day: sleep quality and soreness (1 to 5), the location, and the training tier it picked")
    public CheckInResponse getCheckin(@McpToolParam(description = "Day as YYYY-MM-DD. Leave out for today (UTC)",
    required = false) String date) {
        return caller.call("get_checkin", McpScope.READ, "date=" + date, principal ->
                checkIns.get(principal.userId(), dateOr(date, today())));
    }

    @McpTool(name="list_logs", annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false),
            description = "Logged sets, newest day first: date, workout, exercise, set number, reps or seconds, load in lb, and whether the set was clean")
    public PageResponse<HistoryRow> listLogs(
            @McpToolParam(description = "First day as YYYY-MM-DD", required = false) String from,
            @McpToolParam(description = "Last day as YYYY-MM-DD", required = false) String to,
            @McpToolParam(description = "Only this exercise; ids come from search_exercises", required = false) Long exerciseId,
            @McpToolParam(description = "Page number from 0", required = false) Integer page,
            @McpToolParam(description = "Rows per page, 1 to 100. Default 20", required = false) Integer size) {
        String detail = "from=" + from + "to=" + to + " exerciseId=" + exerciseId + " page=" + page;
        return caller.call("list_logs", McpScope.READ, detail, principal -> {
            // same limits as HistorySearch
            Require.that(page == null || page >= 0, "Page must be 0 or more");
            Require.that(size == null || (size >= 1 && size <= 100), "Page size must be 1 to 100");
            var search = new HistorySearch(exerciseId, null, dateOr(from, null), dateOr(to, null), page, size);
            return history.search(principal.userId(), search);
        });
    }

    @McpTool(name="get_plan", annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false),
            description = "The plan for one week, Monday to Sunday: each day's mode, workout, location, and whether the member locked it")
    public WeekResponse getPlan(@McpToolParam(description = "Any day in the week as YYYY-MM-DD. Leave out for this week",
            required = false) String week) {
        return caller.call("get_plan", McpScope.READ, "week=" + week, principal ->
                plans.week(principal.userId(), dateOr(week, today())));
    }

    private static LocalDate today() { return LocalDate.now(ZoneOffset.UTC); }

    private static LocalDate dateOr(String text, LocalDate fallback) {
        if(text == null || text.isBlank()) return fallback;
        try {
            return LocalDate.parse(text.trim());
        } catch(DateTimeParseException ex) {
            throw new IllegalArgumentException("Dates must be YYYY-MM-DD, got " + text);
        }
    }
}

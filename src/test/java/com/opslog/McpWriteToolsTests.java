package com.opslog;

import com.jayway.jsonpath.JsonPath;
import com.opslog.auth.UserAccount;
import com.opslog.auth.UserAccountRepository;
import com.opslog.checkin.CheckInRequest;
import com.opslog.checkin.CheckInService;
import com.opslog.location.LocationRepository;
import com.opslog.mcp.AiCoachResponse;
import com.opslog.mcp.AiCoachService;
import com.opslog.mcp.McpAuditEntry;
import com.opslog.mcp.McpAuditService;
import com.opslog.mcp.McpOutcome;
import com.opslog.mcp.McpScope;
import com.opslog.mcp.McpTokenRequest;
import com.opslog.mcp.McpTokenService;
import com.opslog.plan.Actor;
import com.opslog.plan.PlanDayRequest;
import com.opslog.plan.PlanDayResponse;
import com.opslog.plan.PlanDayService;
import com.opslog.plan.PlanMode;
import com.opslog.plan.PlanService;
import com.opslog.plan.ProgramService;
import com.opslog.plan.ProgramTemplateResponse;
import com.opslog.plan.UserProgramRequest;
import com.opslog.session.SessionExerciseResponse;
import com.opslog.session.SessionResponse;
import com.opslog.session.SessionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/* not @Transactional: every call is audited in its own transaction, so these tests clean up after themselves */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class McpWriteToolsTests {

    @Autowired MockMvc mvc;
    @Autowired UserAccountRepository users;
    @Autowired McpTokenService tokenService;
    @Autowired McpAuditService audit;
    @Autowired ProgramService programs;
    @Autowired PlanService plans;
    @Autowired PlanDayService planDays;
    @Autowired CheckInService checkIns;
    @Autowired LocationRepository locations;
    @Autowired SessionService sessions;
    @Autowired AiCoachService coach;

    private final LocalDate today = LocalDate.now(ZoneOffset.UTC);
    private final LocalDate tomorrow = LocalDate.now(ZoneOffset.UTC).plusDays(1);

    private UserAccount owner;
    private ProgramTemplateResponse template;

    @BeforeEach
    void setUp() {
        owner = users.save(new UserAccount("write-tools@example.com", "x".repeat(60), "Owner"));
        template = programs.templates().getFirst();
        programs.choose(owner.getId(), new UserProgramRequest(template.id(), null));
    }

    @AfterEach
    void tearDown() {
        users.deleteById(owner.getId());
    }

    private String token(McpScope scope) {
        return tokenService.issue(owner.getId(), new McpTokenRequest("Claude " + scope, scope)).secret();
    }

    private MvcResult send(String secret, String sessionId, String body) throws Exception {
        var request = post("/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ACCEPT, "application/json, text/event-stream")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + secret)
                .content(body);
        if(sessionId != null) request.header("Mcp-Session-Id", sessionId);
        MvcResult result = mvc.perform(request).andReturn();
        /* a tool call answers as a server-sent event, which finishes on an async dispatch */
        return result.getRequest().isAsyncStarted() ? mvc.perform(asyncDispatch(result)).andExpect(status().isOk()).andReturn() : result;
    }

    /* opens an MCP session with the token, makes one tool call, and returns the JSON-RPC result */
    private String call(String secret, String tool, String arguments) throws Exception {
        String session = send(secret, null, """
                {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18",
                 "capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}""")
                .getResponse().getHeader("Mcp-Session-Id");
        send(secret, session, """
                {"jsonrpc":"2.0","method":"notifications/initialized"}""");
        String body = send(secret, session, """
                {"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"%s","arguments":%s}}"""
                .formatted(tool, arguments)).getResponse().getContentAsString();
        return body.lines().filter(line -> line.startsWith("data:")).findFirst().orElseThrow().substring(5);
    }

    private PlanDayResponse planFor(LocalDate date) {
        return plans.week(owner.getId(), date).days().stream()
                .filter(day -> day.date().equals(date)).findFirst().orElseThrow();
    }

    @Test
    void readTokenCannotSetAPlanDay() throws Exception {
        String result = call(token(McpScope.READ), "set_plan_day", """
                {"date":"%s","mode":"REST"}""".formatted(tomorrow));

        assertTrue(JsonPath.<Boolean>read(result, "$.result.isError"));
        assertNotEquals(Actor.AGENT, planFor(tomorrow).updatedBy());
        McpAuditEntry entry = audit.recent(owner.getId()).getFirst();
        assertEquals("set_plan_day", entry.getTool());
        assertEquals(McpOutcome.DENIED, entry.getOutcome());
    }

    @Test
    void agentSetsATrainingDayByWorkoutName() throws Exception {
        String workout = template.workouts().getLast().name();

        String result = call(token(McpScope.READ_WRITE), "set_plan_day", """
                {"date":"%s","mode":"training","workout":"%s"}""".formatted(tomorrow, workout.toLowerCase()));

        assertFalse(JsonPath.<Boolean>read(result, "$.result.isError"));
        PlanDayResponse day = planFor(tomorrow);
        assertEquals(PlanMode.TRAINING, day.mode());
        assertEquals(workout, day.workout().name());
        assertEquals(Actor.AGENT, day.updatedBy());
        assertFalse(day.locked());
        assertEquals(McpOutcome.OK, audit.recent(owner.getId()).getFirst().getOutcome());
    }

    @Test
    void agentCannotChangeADayTheMemberLocked() throws Exception {
        planDays.setDay(owner.getId(), tomorrow, new PlanDayRequest(PlanMode.AUTO, null, null, true), Actor.USER);
        PlanDayResponse before = planFor(tomorrow);

        String result = call(token(McpScope.READ_WRITE), "set_plan_day", """
                {"date":"%s","mode":"REST"}""".formatted(tomorrow));

        assertTrue(JsonPath.<Boolean>read(result, "$.result.isError"));
        PlanDayResponse after = planFor(tomorrow);
        assertTrue(after.locked());
        assertEquals(before.mode(), after.mode());
        assertEquals(Actor.USER, after.updatedBy());
        McpAuditEntry entry = audit.recent(owner.getId()).getFirst();
        assertEquals(McpOutcome.FAILED, entry.getOutcome());
        assertEquals("The plan for " + tomorrow + " is locked", entry.getDetail());
    }

    @Test
    void unknownWorkoutListsTheProgramsWorkouts() throws Exception {
        String result = call(token(McpScope.READ_WRITE), "set_plan_day", """
                {"date":"%s","mode":"TRAINING","workout":"Leg Day"}""".formatted(tomorrow));

        assertTrue(JsonPath.<Boolean>read(result, "$.result.isError"));
        String message = JsonPath.read(result, "$.result.content[0].text");
        assertTrue(message.contains("Unknown workout Leg Day. Use one of:"));
        assertTrue(message.contains(template.workouts().getFirst().name()));
    }

    /* today as a checked-in training day; the rules engine's session gives exercise ids that fit the location */
    private SessionResponse checkedInTrainingDay() {
        long workout = template.workouts().getFirst().id();
        planDays.setDay(owner.getId(), today, new PlanDayRequest(PlanMode.TRAINING, workout, null, false), Actor.USER);
        long downtown = locations.findVisibleTo(owner.getId()).stream()
                .filter(l -> l.getName().equals("Kaimana Downtown")).findFirst().orElseThrow().getId();
        checkIns.save(owner.getId(), today, new CheckInRequest(5, 1, downtown));
        return sessions.get(owner.getId(), today);
    }

    private static String twoExercises(SessionResponse rules) {
        List<SessionExerciseResponse> items = rules.exercises();
        return """
                {"exercises":[{"exerciseId":%d,"sets":4,"reason":"aim for 8 reps"},{"exerciseId":%d,"sets":2}],
                 "notes":["Slept well, so one extra set on the first lift"]}"""
                .formatted(items.get(0).exerciseId(), items.get(1).exerciseId());
    }

    @Test
    void agentWritesTheSessionForACheckedInDay() throws Exception {
        SessionResponse rules = checkedInTrainingDay();

        String result = call(token(McpScope.READ_WRITE), "write_session", twoExercises(rules));

        assertFalse(JsonPath.<Boolean>read(result, "$.result.isError"));
        SessionResponse written = sessions.get(owner.getId(), today);
        assertEquals(Actor.AGENT, written.source());
        assertEquals(2, written.exercises().size());
        assertEquals(4, written.exercises().getFirst().sets());
        assertEquals(List.of("Written by the AI assistant for the Full tier", "Slept well, so one extra set on the first lift"),
                written.notes());
        McpAuditEntry entry = audit.recent(owner.getId()).getFirst();
        assertEquals("write_session", entry.getTool());
        assertEquals(McpOutcome.OK, entry.getOutcome());
    }

    @Test
    void agentCannotWriteTheSessionOfALockedDay() throws Exception {
        SessionResponse rules = checkedInTrainingDay();
        long workout = template.workouts().getFirst().id();
        planDays.setDay(owner.getId(), today, new PlanDayRequest(PlanMode.TRAINING, workout, null, true), Actor.USER);

        String result = call(token(McpScope.READ_WRITE), "write_session", twoExercises(rules));

        assertTrue(JsonPath.<Boolean>read(result, "$.result.isError"));
        assertEquals(Actor.RULES, sessions.get(owner.getId(), today).source());
        assertEquals("The plan for " + today + " is locked", audit.recent(owner.getId()).getFirst().getDetail());
    }

    @Test
    void writeSessionNeedsACheckIn() throws Exception {
        long workout = template.workouts().getFirst().id();
        planDays.setDay(owner.getId(), tomorrow, new PlanDayRequest(PlanMode.TRAINING, workout, null, false), Actor.USER);

        String result = call(token(McpScope.READ_WRITE), "write_session", """
                {"date":"%s","exercises":[{"exerciseId":1,"sets":3}]}""".formatted(tomorrow));

        assertTrue(JsonPath.<Boolean>read(result, "$.result.isError"));
        assertEquals("Check in for " + tomorrow + " first: the check-in picks the tier",
                audit.recent(owner.getId()).getFirst().getDetail());
    }

    @Test
    void readTokenCannotWriteASession() throws Exception {
        SessionResponse rules = checkedInTrainingDay();

        String result = call(token(McpScope.READ), "write_session", twoExercises(rules));

        assertTrue(JsonPath.<Boolean>read(result, "$.result.isError"));
        assertEquals(Actor.RULES, sessions.get(owner.getId(), today).source());
        assertEquals(McpOutcome.DENIED, audit.recent(owner.getId()).getFirst().getOutcome());
    }

    @Test
    void agentLeavesASummaryAndTheNextOneReplacesIt() throws Exception {
        String secret = token(McpScope.READ_WRITE);

        call(secret, "set_coach_summary", """
                {"summary":"  Four days this week; Friday is lighter after a short night.  "}""");
        String result = call(secret, "set_coach_summary", """
                {"summary":"Rest on Wednesday, then back to Upper B."}""");

        assertFalse(JsonPath.<Boolean>read(result, "$.result.isError"));
        AiCoachResponse status = coach.status(owner.getId());
        assertEquals("Rest on Wednesday, then back to Upper B.", status.summary());
        assertNotNull(status.summaryAt());
        McpAuditEntry entry = audit.recent(owner.getId()).getFirst();
        assertEquals("set_coach_summary", entry.getTool());
        assertEquals(McpOutcome.OK, entry.getOutcome());
    }

    @Test
    void readTokenCannotLeaveASummary() throws Exception {
        String result = call(token(McpScope.READ), "set_coach_summary", """
                {"summary":"Four days this week."}""");

        assertTrue(JsonPath.<Boolean>read(result, "$.result.isError"));
        assertNull(coach.status(owner.getId()).summary());
        assertEquals(McpOutcome.DENIED, audit.recent(owner.getId()).getFirst().getOutcome());
    }

    @Test
    void blankSummaryIsRefused() throws Exception {
        String result = call(token(McpScope.READ_WRITE), "set_coach_summary", """
                {"summary":"   "}""");

        assertTrue(JsonPath.<Boolean>read(result, "$.result.isError"));
        assertEquals("Summary is required", audit.recent(owner.getId()).getFirst().getDetail());
    }
}

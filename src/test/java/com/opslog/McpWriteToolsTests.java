package com.opslog;

import com.jayway.jsonpath.JsonPath;
import com.opslog.auth.UserAccount;
import com.opslog.auth.UserAccountRepository;
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
}

package com.opslog;

import com.jayway.jsonpath.JsonPath;
import com.opslog.auth.UserAccount;
import com.opslog.auth.UserAccountRepository;
import com.opslog.checkin.CheckInRequest;
import com.opslog.checkin.CheckInService;
import com.opslog.location.LocationRepository;
import com.opslog.mcp.McpAuditEntry;
import com.opslog.mcp.McpAuditService;
import com.opslog.mcp.McpOutcome;
import com.opslog.mcp.McpScope;
import com.opslog.mcp.McpTokenRequest;
import com.opslog.mcp.McpTokenService;
import com.opslog.plan.ProgramService;
import com.opslog.plan.ProgramTemplateRepository;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/* not @Transactional: every call is audited in its own transaction, so these tests clean up after themselves */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class McpReadToolsTests {

    @Autowired MockMvc mvc;
    @Autowired UserAccountRepository users;
    @Autowired McpTokenService tokenService;
    @Autowired McpAuditService audit;
    @Autowired CheckInService checkIns;
    @Autowired LocationRepository locations;
    @Autowired ProgramService programs;
    @Autowired ProgramTemplateRepository templates;

    private final LocalDate today = LocalDate.now(ZoneOffset.UTC);

    private UserAccount owner;
    private String secret;
    private String session;

    @BeforeEach
    void setUp() throws Exception {
        owner = users.save(new UserAccount("read-tools@example.com", "x".repeat(60), "Owner"));
        secret = tokenService.issue(owner.getId(), new McpTokenRequest("Claude", McpScope.READ)).secret();
        session = send(null, """
                {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18",
                 "capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}""")
                .getResponse().getHeader("Mcp-Session-Id");
        send(session, """
                {"jsonrpc":"2.0","method":"notifications/initialized"}""");
    }

    @AfterEach
    void tearDown() {
        users.deleteById(owner.getId());
    }

    private MvcResult send(String sessionId, String body) throws Exception {
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

    /* the JSON-RPC result of one tool call, from the "data:" line of the event */
    private String call(String tool, String arguments) throws Exception {
        String body = send(session, """
                {"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"%s","arguments":%s}}"""
                .formatted(tool, arguments)).getResponse().getContentAsString();
        return body.lines().filter(line -> line.startsWith("data:")).findFirst().orElseThrow().substring(5);
    }

    private long downtown() {
        return locations.findVisibleTo(owner.getId()).stream()
                .filter(l -> l.getName().equals("Kaimana Downtown")).findFirst().orElseThrow().getId();
    }

    @Test
    void getCheckinReturnsTodaysCheckInAndIsAudited() throws Exception {
        checkIns.save(owner.getId(), today, new CheckInRequest(5, 1, downtown()));

        String result = call("get_checkin", "{}");

        assertFalse(JsonPath.<Boolean>read(result, "$.result.isError"));
        String checkIn = JsonPath.read(result, "$.result.content[0].text");
        assertEquals("Full", JsonPath.read(checkIn, "$.tier.name"));
        assertEquals(today.toString(), JsonPath.read(checkIn, "$.date"));
        McpAuditEntry entry = audit.recent(owner.getId()).getFirst();
        assertEquals("get_checkin", entry.getTool());
        assertEquals(McpOutcome.OK, entry.getOutcome());
    }

    @Test
    void toolCallRecordsTheClientNameOnTheToken() throws Exception {
        call("get_plan", "{}");

        // the test client introduces itself as "test" in the initialize request
        assertEquals("test", tokenService.list(owner.getId()).getFirst().clientName());
    }

    @Test
    void getPlanReturnsTheWeek() throws Exception {
        long template = templates.findAll().getFirst().getId();
        programs.choose(owner.getId(), new UserProgramRequest(template, null));

        String week = JsonPath.read(call("get_plan", "{}"), "$.result.content[0].text");

        assertEquals(7, JsonPath.<List<?>>read(week, "$.days").size());
    }

    @Test
    void listLogsReturnsAnEmptyPageForANewMember() throws Exception {
        String page = JsonPath.read(call("list_logs", """
                {"from":"%s","to":"%s"}""".formatted(today.minusDays(7), today)), "$.result.content[0].text");

        assertEquals(0, JsonPath.<Integer>read(page, "$.totalElements"));
    }

    @Test
    void badInputIsAToolErrorAndIsAuditedAsFailed() throws Exception {
        String result = call("get_checkin", """
                {"date":"tomorrow"}""");

        assertTrue(JsonPath.<Boolean>read(result, "$.result.isError"));
        McpAuditEntry entry = audit.recent(owner.getId()).getFirst();
        assertEquals(McpOutcome.FAILED, entry.getOutcome());
        assertEquals("Dates must be YYYY-MM-DD, got tomorrow", entry.getDetail());
    }

    @Test
    void searchExercisesFiltersByMuscleGroupName() throws Exception {
        String page = JsonPath.read(call("search_exercises", """
                {"muscleGroup":"quadriceps","kind":"STRENGTH"}"""), "$.result.content[0].text");

        List<String> muscles = JsonPath.read(page, "$.items[*].primaryMuscle.name");
        assertTrue(muscles.size() > 1);
        assertTrue(muscles.stream().allMatch("Quadriceps"::equals));
        assertEquals("search_exercises", audit.recent(owner.getId()).getFirst().getTool());
    }

    @Test
    void searchExercisesWithAnUnknownNameListsTheValidOnes() throws Exception {
        String result = call("search_exercises", """
                {"muscleGroup":"Quads"}""");

        assertTrue(JsonPath.<Boolean>read(result, "$.result.isError"));
        String message = JsonPath.read(result, "$.result.content[0].text");
        assertTrue(message.contains("Unknown muscle group Quads. Use one of:"));
        assertTrue(message.contains("Quadriceps"));
        assertEquals(McpOutcome.FAILED, audit.recent(owner.getId()).getFirst().getOutcome());
    }

    @Test
    void getTrainingReportHasATitleAndTimestamp() throws Exception {
        String report = JsonPath.read(call("get_training_report", """
                {"from":"%s","to":"%s"}""".formatted(today.minusDays(6), today)), "$.result.content[0].text");

        assertEquals("Training Report", JsonPath.read(report, "$.title"));
        assertNotNull(JsonPath.read(report, "$.generatedAt"));
        assertEquals(0, JsonPath.<Integer>read(report, "$.sessions"));
        McpAuditEntry entry = audit.recent(owner.getId()).getFirst();
        assertEquals("get_training_report", entry.getTool());
        assertEquals(McpOutcome.OK, entry.getOutcome());
    }
}

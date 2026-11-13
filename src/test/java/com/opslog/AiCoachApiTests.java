package com.opslog;

import com.opslog.auth.UserAccount;
import com.opslog.auth.UserAccountRepository;
import com.opslog.mcp.AiCoachService;
import com.opslog.mcp.IssuedMcpTokenResponse;
import com.opslog.mcp.McpAuditService;
import com.opslog.mcp.McpOutcome;
import com.opslog.mcp.McpPrincipal;
import com.opslog.mcp.McpScope;
import com.opslog.mcp.McpToken;
import com.opslog.mcp.McpTokenRepository;
import com.opslog.mcp.McpTokenRequest;
import com.opslog.mcp.McpTokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.time.Instant;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/* not @Transactional: audit rows commit in their own transaction, so the users are deleted afterwards (everything cascades) */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AiCoachApiTests {

    @Autowired MockMvc mvc;
    @Autowired UserAccountRepository users;
    @Autowired McpTokenService tokenService;
    @Autowired McpTokenRepository tokens;
    @Autowired McpAuditService audit;
    @Autowired AiCoachService coach;

    private UserAccount owner;
    private UserAccount other;

    @BeforeEach
    void setUp() {
        owner = users.save(new UserAccount("coach-owner@example.com", "x".repeat(60), "Owner"));
        other = users.save(new UserAccount("coach-other@example.com", "x".repeat(60), "Other"));
    }

    @AfterEach
    void tearDown() {
        users.deleteById(owner.getId());
        users.deleteById(other.getId());
    }

    private ResultActions coachOf(UserAccount user) throws Exception {
        return mvc.perform(get("/api/ai-coach").with(jwt().jwt(token -> token.subject(user.getId().toString()))));
    }

    /* a token Claude has used: authenticate() stamps last used, recordClient() saves the name */
    private IssuedMcpTokenResponse usedToken(UserAccount user) {
        IssuedMcpTokenResponse issued = tokenService.issue(user.getId(), new McpTokenRequest("Claude", McpScope.READ_WRITE));
        tokenService.authenticate(issued.secret());
        tokenService.recordClient(issued.token().id(), "Claude");
        return issued;
    }

    @Test
    void noTokenMeansInactive() throws Exception {
        coachOf(owner)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.clientName").value(nullValue()))
                .andExpect(jsonPath("$.summary").value(nullValue()))
                .andExpect(jsonPath("$.recent.length()").value(0));
    }

    @Test
    void recentlyUsedTokenIsActiveWithItsClientAndActivity() throws Exception {
        IssuedMcpTokenResponse issued = usedToken(owner);
        var caller = new McpPrincipal(owner.getId(), issued.token().id(), McpScope.READ_WRITE);
        audit.record(caller, "get_plan", McpOutcome.OK, "week=null");
        audit.record(caller, "set_plan_day", McpOutcome.FAILED, "The plan for 2026-11-14 is locked");

        coachOf(owner)
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.clientName").value("Claude"))
                .andExpect(jsonPath("$.lastActiveAt").isNotEmpty())
                .andExpect(jsonPath("$.recent.length()").value(2))
                .andExpect(jsonPath("$.recent[0].tool").value("set_plan_day"))
                .andExpect(jsonPath("$.recent[0].outcome").value("FAILED"));
    }

    @Test
    void revokedTokenIsInactiveRightAway() throws Exception {
        IssuedMcpTokenResponse issued = usedToken(owner);
        tokenService.revoke(owner.getId(), issued.token().id());

        coachOf(owner).andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void tokenLastUsedEightDaysAgoIsInactive() throws Exception {
        IssuedMcpTokenResponse issued = usedToken(owner);
        McpToken token = tokens.findById(issued.token().id()).orElseThrow();
        token.markUsed(Instant.now().minus(Duration.ofDays(8)));
        tokens.save(token);

        coachOf(owner)
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.clientName").value("Claude"));
    }

    @Test
    void anotherMembersTokenIsNeverVisible() throws Exception {
        usedToken(other);

        coachOf(owner)
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.clientName").value(nullValue()));
    }

    @Test
    void summaryIsTheMembersOwn() throws Exception {
        coach.setSummary(other.getId(), "Other member's week");
        coach.setSummary(owner.getId(), "Four days this week");

        coachOf(owner)
                .andExpect(jsonPath("$.summary").value("Four days this week"))
                .andExpect(jsonPath("$.summaryAt").isNotEmpty());
    }
}

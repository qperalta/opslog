package com.opslog;

import com.opslog.auth.TokenService;
import com.opslog.auth.UserAccount;
import com.opslog.auth.UserAccountRepository;
import com.opslog.mcp.McpScope;
import com.opslog.mcp.McpTokenRepository;
import com.opslog.mcp.McpTokenRequest;
import com.opslog.mcp.McpTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class McpSecurityTests {

    /* the first message every MCP client sends */
    private static final String INITIALIZE = """
            {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18",
             "capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}""";

    @Autowired MockMvc mvc;
    @Autowired UserAccountRepository users;
    @Autowired McpTokenService tokenService;
    @Autowired McpTokenRepository tokens;
    @Autowired TokenService loginTokens;

    private UserAccount owner;

    @BeforeEach
    void setUp() {
        owner = users.save(new UserAccount("owner@example.com", "x".repeat(60), "Owner"));
    }

    private String secret(McpScope scope) {
        return tokenService.issue(owner.getId(), new McpTokenRequest("Claude", scope)).secret();
    }

    private ResultActions initialize(String authorization) throws Exception {
        var request = post("/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ACCEPT, "application/json, text/event-stream")
                .content(INITIALIZE);
        if(authorization != null) request.header(HttpHeaders.AUTHORIZATION, authorization);
        return mvc.perform(request);
    }

    @Test
    void mcpNeedsAToken() throws Exception {
        initialize(null)
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")));
    }

    @Test
    void unknownTokenIsRejected() throws Exception {
        initialize("Bearer opslog_not-a-real-token").andExpect(status().isUnauthorized());
    }

    @Test
    void loginJwtIsNotAnMcpToken() throws Exception {
        String loginJwt = loginTokens.issue(owner).getTokenValue();

        initialize("Bearer " + loginJwt).andExpect(status().isUnauthorized());
    }

    @Test
    void validTokenReachesTheMcpServerAndIsMarkedUsed() throws Exception {
        String secret = secret(McpScope.READ);

        initialize("Bearer " + secret).andExpect(status().isOk());

        assertNotNull(tokens.findAll().getFirst().getLastUsedAt());
    }

    @Test
    void revokedTokenIsRejected() throws Exception {
        String secret = secret(McpScope.READ_WRITE);
        long id = tokens.findAll().getFirst().getId();
        tokenService.revoke(owner.getId(), id);

        initialize("Bearer " + secret).andExpect(status().isUnauthorized());
    }

    @Test
    void mcpTokenDoesNotOpenTheAppApi() throws Exception {
        String secret = secret(McpScope.READ_WRITE);

        mvc.perform(get("/api/mcp-tokens").header(HttpHeaders.AUTHORIZATION, "Bearer " + secret))
                .andExpect(status().isUnauthorized());
    }
}
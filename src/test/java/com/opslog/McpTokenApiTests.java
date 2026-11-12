package com.opslog;

import com.jayway.jsonpath.JsonPath;
import com.opslog.auth.UserAccount;
import com.opslog.auth.UserAccountRepository;
import com.opslog.mcp.McpTokenRepository;
import com.opslog.mcp.TokenHash;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class McpTokenApiTests {

    @Autowired MockMvc mvc;
    @Autowired UserAccountRepository users;
    @Autowired McpTokenRepository tokens;

    private UserAccount owner;
    private UserAccount other;

    @BeforeEach
    void setUp() {
        owner = users.save(new UserAccount("owner@example.com", "x".repeat(60), "Owner"));
        other = users.save(new UserAccount("other@example.com", "x".repeat(60), "Other"));
    }

    private RequestPostProcessor as(UserAccount user) {
        return jwt().jwt(token -> token.subject(user.getId().toString()));
    }

    private ResultActions issue(UserAccount user, String body) throws Exception {
        return mvc.perform(post("/api/mcp-tokens").with(as(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private long issuedId(UserAccount user, String name) throws Exception {
        String body = issue(user, """
                {"name":"%s","scope":"READ"}""".formatted(name))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.token.id")).longValue();
    }

    @Test
    void tokensNeedASignedInUser() throws Exception {
        mvc.perform(get("/api/mcp-tokens")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/mcp-tokens").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"x\",\"scope\":\"READ\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void issuedSecretIsShownOnceAndOnlyItsHashIsStored() throws Exception {
        String body = issue(owner, """
                {"name":"  Claude desktop ","scope":"READ_WRITE"}""")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/mcp-tokens/")))
                .andExpect(jsonPath("$.token.name").value("Claude desktop"))
                .andExpect(jsonPath("$.token.scope").value("READ_WRITE"))
                .andExpect(jsonPath("$.token.active").value(true))
                .andExpect(jsonPath("$.secret", startsWith("opslog_")))
                .andReturn().getResponse().getContentAsString();

        String secret = JsonPath.read(body, "$.secret");
        assertTrue(tokens.findByTokenHash(TokenHash.of(secret)).isPresent());
        assertTrue(tokens.findAll().stream().noneMatch(token -> token.getTokenHash().equals(secret)));

        mvc.perform(get("/api/mcp-tokens").with(as(owner)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].secret").doesNotExist())
                .andExpect(jsonPath("$[0].tokenHash").doesNotExist());
    }

    @Test
    void everyTokenIsDifferent() throws Exception {
        String first = JsonPath.read(issue(owner, "{\"name\":\"One\",\"scope\":\"READ\"}").andReturn().getResponse().getContentAsString(), "$.secret");
        String second = JsonPath.read(issue(owner, "{\"name\":\"Two\",\"scope\":\"READ\"}").andReturn().getResponse().getContentAsString(), "$.secret");

        assertNotEquals(first, second);
    }

    @Test
    void nameAndScopeAreValidated() throws Exception {
        issue(owner, """
                {"name":"   ","scope":"READ"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("Name is required"));
        issue(owner, """
                {"name":"%s","scope":"READ"}""".formatted("a".repeat(61)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("Name must be 60 characters or fewer"));
        issue(owner, """
                {"name":"Claude"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.scope").value("Scope is required"));
        issue(owner, """
                {"name":"Claude","scope":"ADMIN"}""")
                .andExpect(status().isBadRequest());
    }

    @Test
    void listShowsOnlyYourOwnTokensNewestFirst() throws Exception {
        issuedId(owner, "First");
        issuedId(owner, "Second");
        issuedId(other, "Not yours");

        mvc.perform(get("/api/mcp-tokens").with(as(owner)))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("First", "Second")));
    }

    @Test
    void revokedTokenStaysListedButInactive() throws Exception {
        long id = issuedId(owner, "Claude");

        mvc.perform(post("/api/mcp-tokens/" + id + "/revoke").with(as(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.revokedAt").exists());

        mvc.perform(get("/api/mcp-tokens").with(as(owner)))
                .andExpect(jsonPath("$[0].active").value(false));
    }

    @Test
    void anotherUsersTokenCannotBeRevoked() throws Exception {
        long id = issuedId(owner, "Claude");

        mvc.perform(post("/api/mcp-tokens/" + id + "/revoke").with(as(other)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Token " + id + " was not found"));

        mvc.perform(get("/api/mcp-tokens").with(as(owner)))
                .andExpect(jsonPath("$[0].active").value(true));
    }
}
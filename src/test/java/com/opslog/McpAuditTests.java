package com.opslog;

import com.opslog.auth.UserAccount;
import com.opslog.auth.UserAccountRepository;
import com.opslog.mcp.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/* not @Transactional: record() commits in its own transaction, so these tests clean up after themselves */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class McpAuditTests {

    @Autowired UserAccountRepository users;
    @Autowired McpTokenService tokenService;
    @Autowired McpTokenRepository tokens;
    @Autowired McpAuditService audit;
    @Autowired McpAuditRepository entries;
    @Autowired TransactionTemplate transaction;

    private UserAccount owner;
    private McpPrincipal caller;

    @BeforeEach
    void setUp() {
        owner = users.save(new UserAccount("audit-owner@example.com", "x".repeat(60), "Owner"));
        tokenService.issue(owner.getId(), new McpTokenRequest("Claude", McpScope.READ_WRITE));
        McpToken token = tokens.findByUserIdOrderByCreatedAtDesc(owner.getId()).getFirst();
        caller = new McpPrincipal(owner.getId(), token.getId(), token.getScope());
    }

    @AfterEach
    void tearDown() {
        /* deleting the user cascades to its tokens and audit rows */
        users.deleteById(owner.getId());
    }

    @Test
    void callsAreListedNewestFirstForTheirOwner() {
        audit.record(caller, "get_plan", McpOutcome.OK, null);
        audit.record(caller, "set_plan_day", McpOutcome.DENIED, "Token is read only");

        List<McpAuditEntry> recent = audit.recent(owner.getId());

        assertEquals(List.of("set_plan_day", "get_plan"), recent.stream().map(McpAuditEntry::getTool).toList());
        assertEquals(McpOutcome.DENIED, recent.getFirst().getOutcome());
        assertEquals(caller.tokenId(), recent.getFirst().getTokenId());
    }

    @Test
    void auditRowSurvivesTheToolRollingBack() {
        assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
            audit.record(caller, "write_session", McpOutcome.FAILED, "Day is locked");
            throw new IllegalStateException("the tool failed");
        }));

        assertEquals(1, entries.findTop50ByUserIdOrderByCreatedAtDesc(owner.getId()).size());
    }
}
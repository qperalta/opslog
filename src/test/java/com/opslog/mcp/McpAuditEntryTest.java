package com.opslog.mcp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class McpAuditEntryTest {

    private static final McpPrincipal CALLER = new McpPrincipal(7L, 3L, McpScope.READ);

    @Test void callerIsCopiedFromThePrincipal() {
        McpAuditEntry entry = new McpAuditEntry(CALLER, "get_plan", McpOutcome.OK, null);

        assertEquals(7L, entry.getUserId());
        assertEquals(3L, entry.getTokenId());
        assertEquals("get_plan", entry.getTool());
        assertNotNull(entry.getCreatedAt());
    }

    @Test void longDetailIsCutToTheColumn() {
        McpAuditEntry entry = new McpAuditEntry(CALLER, "write_session", McpOutcome.FAILED, "x".repeat(500));

        assertEquals(200, entry.getDetail().length());
        assertTrue(entry.getDetail().endsWith("..."));
    }

    @Test void toolIsRequired() {
        assertThrows(IllegalArgumentException.class, () -> new McpAuditEntry(CALLER, " ", McpOutcome.OK, null));
    }
}

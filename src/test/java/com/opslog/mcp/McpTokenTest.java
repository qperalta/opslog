package com.opslog.mcp;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class McpTokenTest {

    private static final String HASH = "a".repeat(64);

    @Test
    void nameIsTrimmed() {
        McpToken token = new McpToken(1L, "  Claude desktop ", HASH, McpScope.READ);

        assertEquals("Claude desktop", token.getName());
        assertTrue(token.isActive());
    }

    @Test
    void blankNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new McpToken(1L, "   ", HASH, McpScope.READ));
    }

    @Test
    void hashMustBeSha256Hex() {
        assertThrows(IllegalArgumentException.class, () -> new McpToken(1L, "Claude", "raw-token", McpScope.READ));
    }

    @Test
    void readTokenCannotWrite() {
        McpToken token = new McpToken(1L, "Claude", HASH, McpScope.READ);

        assertTrue(token.allows(McpScope.READ));
        assertFalse(token.allows(McpScope.READ_WRITE));
    }

    @Test
    void readWriteTokenCanReadAndWrite() {
        McpToken token = new McpToken(1L, "Claude", HASH, McpScope.READ_WRITE);

        assertTrue(token.allows(McpScope.READ));
        assertTrue(token.allows(McpScope.READ_WRITE));
    }

    @Test
    void revokedTokenAllowsNothing() {
        McpToken token = new McpToken(1L, "Claude", HASH, McpScope.READ_WRITE);
        Instant first = Instant.parse("2026-11-11T10:00:00Z");

        token.revoke(first);
        token.revoke(first.plusSeconds(60));

        assertFalse(token.isActive());
        assertFalse(token.allows(McpScope.READ));
        assertEquals(first, token.getRevokedAt());
        assertThrows(IllegalArgumentException.class, () -> token.markUsed(Instant.now()));
    }

    @Test
    void recordClientTrimsAndIgnoresBlank() {
        McpToken token = new McpToken(1L, "Claude", HASH, McpScope.READ);

        token.recordClient("  Claude  ");
        assertEquals("Claude", token.getClientName());

        token.recordClient("   ");
        token.recordClient(null);
        assertEquals("Claude", token.getClientName());
    }

    @Test
    void recordClientCutsLongNamesToTheColumn() {
        McpToken token = new McpToken(1L, "Claude", HASH, McpScope.READ);

        token.recordClient("x".repeat(80));

        assertEquals(60, token.getClientName().length());
    }
}

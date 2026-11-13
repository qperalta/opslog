package com.opslog.mcp;

import java.time.Instant;

/* one row of the audit log, as the member sees it */
public record AiActivityResponse(String tool, McpOutcome outcome, String detail, Instant at) {

    static AiActivityResponse from(McpAuditEntry entry) {
        return new AiActivityResponse(entry.getTool(), entry.getOutcome(), entry.getDetail(), entry.getCreatedAt());
    }
}

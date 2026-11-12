package com.opslog.mcp;

import java.time.Instant;

public record McpTokenResponse(
        long id,
        String name,
        McpScope scope,
        boolean active,
        Instant createdAt,
        Instant lastUsedAt,
        Instant revokedAt
) {

    static McpTokenResponse from(McpToken token) {
        return new McpTokenResponse(token.getId(), token.getName(), token.getScope(), token.isActive(),
                token.getCreatedAt(), token.getLastUsedAt(), token.getRevokedAt());
    }
}

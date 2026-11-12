package com.opslog.mcp;

public record McpPrincipal(long userId, long tokenId, McpScope scope) {}

package com.opslog.mcp;

public record IssuedMcpTokenResponse(McpTokenResponse token, String secret) {}

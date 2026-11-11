package com.opslog.mcp;

public enum McpScope {
    READ,
    READ_WRITE;

    public boolean allows(McpScope needed) { return this == READ_WRITE || needed == READ; }
}

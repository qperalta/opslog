package com.opslog.mcp;

public class McpTokenNotFoundException  extends RuntimeException {

    public McpTokenNotFoundException(long id) { super("Token " + id + " was not found"); }
}

package com.opslog.mcp;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class PingTool {

    @McpTool(name = "ping", description = "Health check. Returns server status and the current server time in UTC")
    public String ping() {
        return "OpsLog MCP server v1.0.0 OK at " + Instant.now().toString();
    }

}

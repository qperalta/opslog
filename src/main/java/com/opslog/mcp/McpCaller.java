package com.opslog.mcp;

import org.springframework.ai.mcp.annotation.context.McpSyncRequestContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class McpCaller {

    private final McpAuditService audit;
    private final McpTokenService tokens;

    McpCaller(McpAuditService audit, McpTokenService tokens) {
        this.audit = audit;
        this.tokens = tokens;
    }

    public <T> T call(McpSyncRequestContext context, String tool, McpScope needed, String detail, Function<McpPrincipal, T> work) {
        McpPrincipal caller = current();
        tokens.recordClient(caller.tokenId(), clientName(context));
        if(!caller.scope().allows(needed)) {
            audit.record(caller, tool, McpOutcome.DENIED, detail);
            throw new IllegalStateException("This token can only read. Issue a read and write token to use " + tool);
        }
        try {
            T result = work.apply(caller);
            audit.record(caller, tool, McpOutcome.OK, detail);
            return result;
        } catch(RuntimeException ex) {
            audit.record(caller, tool, McpOutcome.FAILED, ex.getMessage());
            throw ex;
        }
    }

    private static McpPrincipal current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication != null && authentication.getPrincipal() instanceof McpPrincipal principal) return principal;
        throw new IllegalStateException("This call has no MCP token");
    }

    private static String clientName(McpSyncRequestContext context) {
        if(context == null || context.clientInfo() == null) return null;
        var info = context.clientInfo();
        return info.title() != null && !info.title().isBlank()
                ? info.title()
                : info.name();
    }
}

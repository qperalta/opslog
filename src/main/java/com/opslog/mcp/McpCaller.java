package com.opslog.mcp;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class McpCaller {

    private final McpAuditService audit;

    McpCaller(McpAuditService audit) { this.audit = audit; }

    public <T> T call(String tool, McpScope needed, String detail, Function<McpPrincipal, T> work) {
        McpPrincipal caller = current();
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
}

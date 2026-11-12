package com.opslog.mcp;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class McpAuditService {

    private final McpAuditRepository entries;

    McpAuditService(McpAuditRepository entries) { this.entries = entries; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(McpPrincipal caller, String tool, McpOutcome outcome, String detail) {
        entries.save(new McpAuditEntry(caller, tool, outcome, detail));
    }

    @Transactional(readOnly = true)
    public List<McpAuditEntry> recent(long userId) {
        return entries.findTop50ByUserIdOrderByCreatedAtDesc(userId);
    }
}

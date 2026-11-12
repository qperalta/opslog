package com.opslog.mcp;

import com.opslog.common.Require;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name="mcp_audit")
public class McpAuditEntry {

    private static final int MAX_TOOL = 40;
    private static final int MAX_DETAIL = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;
    @Column(name = "token_id", nullable = false, updatable = false)
    private Long tokenId;
    @Column(nullable = false, updatable = false, length = MAX_TOOL)
    private String tool;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 10)
    private McpOutcome outcome;
    @Column(updatable = false, length = MAX_DETAIL)
    private String detail;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected McpAuditEntry() {
    }

    public McpAuditEntry(McpPrincipal caller, String tool, McpOutcome outcome, String detail) {
        Require.that(caller != null, "Caller is required");
        Require.that(tool != null && !tool.isBlank() && tool.length() <= MAX_TOOL, "Tool must be 1 to " + MAX_TOOL + " characters");
        Require.that(outcome != null, "Outcome is required");

        this.userId = caller.userId();
        this.tokenId = caller.tokenId();
        this.tool = tool;
        this.outcome = outcome;
        this.detail = detail == null || detail.length() <= MAX_DETAIL
                ? detail
                : detail.substring(0, MAX_DETAIL - 3) + "...";
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getTokenId() { return tokenId; }
    public String getTool() { return tool; }
    public McpOutcome getOutcome() { return outcome; }
    public String getDetail() { return detail; }
    public Instant getCreatedAt() { return createdAt; }
}
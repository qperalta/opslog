package com.opslog.mcp;

import com.opslog.common.Require;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name="mcp_tokens")
public class McpToken {

    private static final int MAX_NAME = 60;
    private static final String SHA_256_HEX = "[0-9a-f]{64}";

    @Id @GeneratedValue(strategy= GenerationType.IDENTITY) private Long id;
    @Column(name="user_id", nullable = false, updatable = false) private Long userId;
    @Column(nullable = false, length=MAX_NAME) private String name;
    @Column(name="token_hash", nullable = false, updatable = false, length=64) private String tokenHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length=10) private McpScope scope;
    @Column(name="created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name="last_used_at") private Instant lastUsedAt;
    @Column(name="revoked_at") private Instant revokedAt;

    protected McpToken() {}

    public McpToken(Long userId, String name, String tokenHash, McpScope scope) {
        Require.that(userId != null, "User is required");
        Require.that(name != null && !name.isBlank(), "Name is required");
        Require.that(name.trim().length() <= MAX_NAME, "Name must be " + MAX_NAME + " characters or fewer");
        Require.that(tokenHash != null && tokenHash.matches(SHA_256_HEX), "Token hash must be a SHA-256 hex");
        Require.that(scope != null, "Scope is required");

        this.userId = userId;
        this.name = name.trim();
        this.tokenHash = tokenHash;
        this.scope = scope;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getName() { return name; }
    public String getTokenHash() { return tokenHash; }
    public McpScope getScope() { return scope; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastUsedAt() { return lastUsedAt; }
    public Instant getRevokedAt() { return revokedAt; }

    public boolean isActive() { return revokedAt == null; }

    // we need to set a revokedToken to never be used again
    public void revoke(Instant when) {
        Require.that(when != null, "Revoke time is required");
        if(revokedAt == null) revokedAt = when;
    }

    public void markUsed(Instant when) {
        Require.that(isActive(), "A revoked token cannot be used");
        lastUsedAt = when;
    }

    public boolean allows(McpScope needed) { return isActive() && scope.allows(needed); }
}

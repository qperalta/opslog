package com.opslog.mcp;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface McpAuditRepository extends JpaRepository<McpAuditEntry, Long> {
    List<McpAuditEntry> findTop50ByUserIdOrderByCreatedAtDesc(long userId);

    List<McpAuditEntry> findTop5ByUserIdOrderByCreatedAtDesc(long userId);
}

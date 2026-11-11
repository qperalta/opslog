package com.opslog.mcp;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface McpTokenRepository extends JpaRepository<McpToken, Long> {

    Optional<McpToken> findByTokenHash(String tokenHash);

    List<McpToken> findByUserIdOrderByCreatedAtDesc(long userId);

    Optional<McpToken> findByIdAndUserId(long id, long userId);
}

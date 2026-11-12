package com.opslog.mcp;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class McpTokenService {

    static final String PREFIX = "opslog_";
    private static final int RANDOM_BYTES = 32;

    private final McpTokenRepository tokens;
    private final SecureRandom random = new SecureRandom();
    private static final Duration LAST_USED_STEP = Duration.ofMinutes(1);

    McpTokenService(McpTokenRepository tokens) { this.tokens = tokens; }

    public IssuedMcpTokenResponse issue(long userId, McpTokenRequest request) {
        byte[] bytes = new byte[RANDOM_BYTES];
        random.nextBytes(bytes);
        String secret = PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        McpToken saved = tokens.save(new McpToken(userId, request.name(), TokenHash.of(secret), request.scope()));
        return new IssuedMcpTokenResponse(McpTokenResponse.from(saved), secret);
    }

    @Transactional(readOnly = true)
    public List<McpTokenResponse> list(long userId) {
        return tokens.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(McpTokenResponse::from)
                .toList();
    }

    public Optional<McpPrincipal> authenticate(String secret) {
        if(secret == null || !secret.startsWith(PREFIX)) return Optional.empty();
        return tokens.findByTokenHash(TokenHash.of(secret))
                .filter(McpToken::isActive)
                .map(token -> {
                    Instant now = Instant.now();
                    if(token.getLastUsedAt() == null || token.getLastUsedAt().isBefore(now.minus(LAST_USED_STEP))) {
                        token.markUsed(now);
                    }
                    return new McpPrincipal(token.getUserId(), token.getId(), token.getScope());
                });
    }

    public McpTokenResponse revoke(long userId, long id) {
        McpToken token = tokens.findByIdAndUserId(id, userId).orElseThrow(() -> new McpTokenNotFoundException(id));
        token.revoke(Instant.now());
        return McpTokenResponse.from(token);
    }
}

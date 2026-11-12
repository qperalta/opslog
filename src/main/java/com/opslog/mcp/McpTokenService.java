package com.opslog.mcp;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Service
@Transactional
public class McpTokenService {

    static final String PREFIX = "opslog_";
    private static final int RANDOM_BYTES = 32;

    private final McpTokenRepository tokens;
    private final SecureRandom random = new SecureRandom();

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

    public McpTokenResponse revoke(long userId, long id) {
        McpToken token = tokens.findByIdAndUserId(id, userId).orElseThrow(() -> new McpTokenNotFoundException(id));
        token.revoke(Instant.now());
        return McpTokenResponse.from(token);
    }
}

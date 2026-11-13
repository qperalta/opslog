package com.opslog.mcp;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class AiCoachService {

    static final Duration ACTIVE_WINDOW = Duration.ofDays(7);

    private final McpTokenRepository tokens;
    private final McpAuditRepository audit;
    private final CoachSummaryRepository summaries;

    AiCoachService(McpTokenRepository tokens, McpAuditRepository audit, CoachSummaryRepository summaries) {
        this.tokens = tokens;
        this.audit = audit;
        this.summaries = summaries;
    }

    public AiCoachResponse status(long userId) {
        Instant since = Instant.now().minus(ACTIVE_WINDOW);
        // the most recent used not revoked
        Optional<McpToken> latest = tokens.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(McpToken::isActive)
                .filter(token -> token.getLastUsedAt() != null)
                .max(Comparator.comparing(McpToken::getLastUsedAt));
        boolean active = latest.map(token -> token.getLastUsedAt().isAfter(since)).orElse(false);
        List<AiActivityResponse> recent = audit.findTop5ByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(AiActivityResponse::from)
                .toList();
        Optional<CoachSummary> summary = summaries.findById(userId);
        return new AiCoachResponse(active,
                latest.map(McpToken::getClientName).orElse(null),
                latest.map(McpToken::getLastUsedAt).orElse(null),
                summary.map(CoachSummary::getSummary).orElse(null),
                summary.map(CoachSummary::getUpdatedAt).orElse(null),
                recent);
    }

    // one summary per member: the first call creates it, later calls replace the text
    @Transactional
    public CoachSummaryResponse setSummary(long userId, String text) {
        CoachSummary summary = summaries.findById(userId)
                .map(existing -> {
                    existing.rewrite(text);
                    return existing;
                })
                .orElseGet(() -> new CoachSummary(userId, text));
        return CoachSummaryResponse.from(summaries.save(summary));
    }
}

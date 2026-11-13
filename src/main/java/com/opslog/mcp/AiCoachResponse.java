package com.opslog.mcp;

import java.time.Instant;
import java.util.List;

/* active = a non-revoked token was used in the last 7 days; clientName and lastActiveAt come from the most recently used token */
public record AiCoachResponse(boolean active, String clientName, Instant lastActiveAt, List<AiActivityResponse> recent) {}

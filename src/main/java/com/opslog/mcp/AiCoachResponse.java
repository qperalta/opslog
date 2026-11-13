package com.opslog.mcp;

import java.time.Instant;
import java.util.List;

/* active = a non-revoked token was used in the last 7 days; clientName and lastActiveAt come from the most recently used token;
   summary and summaryAt are the assistant's latest set_coach_summary, null until it writes one */
public record AiCoachResponse(boolean active, String clientName, Instant lastActiveAt, String summary, Instant summaryAt,
                              List<AiActivityResponse> recent) {}

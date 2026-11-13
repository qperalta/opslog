package com.opslog.mcp;

import java.time.Instant;

/* what set_coach_summary hands back to the assistant */
public record CoachSummaryResponse(String summary, Instant updatedAt) {

    static CoachSummaryResponse from(CoachSummary item) {
        return new CoachSummaryResponse(item.getSummary(), item.getUpdatedAt());
    }
}

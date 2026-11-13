package com.opslog.mcp;

import com.opslog.common.Require;
import jakarta.persistence.*;

import java.time.Instant;

/* the assistant's latest note on the member's plan; one per member, each call replaces it */
@Entity
@Table(name="coach_summaries")
public class CoachSummary {

    static final int MAX_SUMMARY = 1000;

    @Id @Column(name="user_id", updatable = false) private Long userId;
    @Column(nullable = false, length=MAX_SUMMARY) private String summary;
    @Column(name="updated_at", nullable = false) private Instant updatedAt;

    protected CoachSummary() {}

    public CoachSummary(Long userId, String summary) {
        Require.that(userId != null, "User is required");
        this.userId = userId;
        rewrite(summary);
    }

    public Long getUserId() { return userId; }
    public String getSummary() { return summary; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void rewrite(String summary) {
        String text = summary == null ? "" : summary.strip();
        Require.that(!text.isEmpty(), "Summary is required");
        Require.that(text.length() <= MAX_SUMMARY, "Summary must be " + MAX_SUMMARY + " characters or fewer");
        this.summary = text;
        this.updatedAt = Instant.now();
    }
}

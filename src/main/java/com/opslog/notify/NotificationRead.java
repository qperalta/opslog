package com.opslog.notify;

import com.opslog.common.Require;
import jakarta.persistence.*;

import java.time.Instant;

/* everything published up to readAt counts as seen by this member */
@Entity
@Table(name="notification_reads")
public class NotificationRead {

    @Id @Column(name="user_id", updatable = false) private Long userId;
    @Column(name="read_at", nullable = false) private Instant readAt;

    protected NotificationRead() {}

    public NotificationRead(Long userId, Instant readAt) {
        Require.that(userId != null, "User is required");
        this.userId = userId;
        markRead(readAt);
    }

    public Long getUserId() { return userId; }
    public Instant getReadAt() { return readAt; }

    // never moves back, so an older request can't bring back seen ones
    public void markRead(Instant when) {
        Require.that(when != null, "Read time is required");
        if(readAt == null || when.isAfter(readAt)) readAt = when;
    }
}

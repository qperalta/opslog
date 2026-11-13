package com.opslog.notify;

import com.opslog.common.Require;
import jakarta.persistence.*;

import java.time.Instant;

/* a what's-new note for every member; published by a migration */
@Entity
@Table(name="announcements")
public class Announcement {

    private static final int MAX_TITLE = 80;
    private static final int MAX_BODY = 500;

    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, updatable = false, length=MAX_TITLE) private String title;
    @Column(nullable = false, updatable = false, length=MAX_BODY) private String body;
    @Column(name="published_at", nullable = false, updatable = false) private Instant publishedAt;

    protected Announcement() {}

    public Announcement(String title, String body, Instant publishedAt) {
        Require.that(title != null && !title.isBlank() && title.strip().length() <= MAX_TITLE, "Title must be 1 to " + MAX_TITLE + " characters");
        Require.that(body != null && !body.isBlank() && body.strip().length() <= MAX_BODY, "Body must be 1 to " + MAX_BODY + " characters");
        Require.that(publishedAt != null, "Published time is required");
        this.title = title.strip();
        this.body = body.strip();
        this.publishedAt = publishedAt;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public Instant getPublishedAt() { return publishedAt; }
}

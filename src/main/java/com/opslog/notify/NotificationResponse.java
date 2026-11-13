package com.opslog.notify;

import java.time.Instant;

public record NotificationResponse(long id, String title, String body, Instant publishedAt, boolean unread) {}

package com.opslog.notify;

import java.util.List;

/* newest first; unreadCount drives the badge on the avatar */
public record NotificationsResponse(long unreadCount, List<NotificationResponse> items) {}

package com.opslog.notify;

import com.opslog.auth.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notifications;
    private final CurrentUser currentUser;

    public NotificationController(NotificationService notifications, CurrentUser currentUser) {
        this.notifications = notifications;
        this.currentUser = currentUser;
    }

    @GetMapping public NotificationsResponse list() { return notifications.list(currentUser.id()); }

    @PostMapping("/read")
    public ResponseEntity<Void> markRead() {
        notifications.markRead(currentUser.id());
        return ResponseEntity.noContent().build();
    }
}

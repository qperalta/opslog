package com.opslog.notify;

import com.opslog.auth.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class NotificationService {

    private final AnnouncementRepository announcements;
    private final NotificationReadRepository reads;
    private final UserAccountRepository users;

    NotificationService(AnnouncementRepository announcements, NotificationReadRepository reads, UserAccountRepository users) {
        this.announcements = announcements;
        this.reads = reads;
        this.users = users;
    }

    /* unread = published after the member last opened the list; before the first time, after they signed up */
    public NotificationsResponse list(long userId) {
        Instant seen = reads.findById(userId)
                .map(NotificationRead::getReadAt)
                .orElseGet(() -> users.findById(userId).orElseThrow().getCreatedAt());
        List<NotificationResponse> items = announcements.findTop20ByOrderByPublishedAtDesc().stream()
                .map(item -> new NotificationResponse(item.getId(), item.getTitle(), item.getBody(), item.getPublishedAt(),
                        item.getPublishedAt().isAfter(seen)))
                .toList();
        return new NotificationsResponse(items.stream().filter(NotificationResponse::unread).count(), items);
    }

    @Transactional
    public void markRead(long userId) {
        Instant now = Instant.now();
        NotificationRead read = reads.findById(userId).orElseGet(() -> new NotificationRead(userId, now));
        read.markRead(now);
        reads.save(read);
    }
}

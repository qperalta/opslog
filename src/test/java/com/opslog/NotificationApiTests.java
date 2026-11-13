package com.opslog;

import com.opslog.auth.UserAccount;
import com.opslog.auth.UserAccountRepository;
import com.opslog.notify.Announcement;
import com.opslog.notify.AnnouncementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NotificationApiTests {

    @Autowired MockMvc mvc;
    @Autowired UserAccountRepository users;
    @Autowired AnnouncementRepository announcements;

    private UserAccount owner;
    private UserAccount other;

    @BeforeEach
    void setUp() {
        owner = users.save(new UserAccount("notify-owner@example.com", "x".repeat(60), "Owner"));
        other = users.save(new UserAccount("notify-other@example.com", "x".repeat(60), "Other"));
    }

    private RequestPostProcessor as(UserAccount user) {
        return jwt().jwt(token -> token.subject(user.getId().toString()));
    }

    private ResultActions listFor(UserAccount user) throws Exception {
        return mvc.perform(get("/api/notifications").with(as(user)));
    }

    /* published just after both members signed up */
    private void publish(String title) {
        announcements.save(new Announcement(title, "Body", other.getCreatedAt().plus(Duration.ofMillis(1))));
    }

    @Test
    void notificationsNeedAToken() throws Exception {
        mvc.perform(get("/api/notifications")).andExpect(status().isUnauthorized());
    }

    @Test
    void olderThanTheAccountIsListedButNotUnread() throws Exception {
        listFor(owner)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0))
                .andExpect(jsonPath("$.items[*].title", hasItem("Add exercises mid-workout")));
    }

    @Test
    void newOneIsUnreadUntilTheListIsOpened() throws Exception {
        publish("Notifications on the avatar");

        listFor(owner)
                .andExpect(jsonPath("$.unreadCount").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Notifications on the avatar"))
                .andExpect(jsonPath("$.items[0].unread").value(true));

        mvc.perform(post("/api/notifications/read").with(as(owner))).andExpect(status().isNoContent());

        listFor(owner)
                .andExpect(jsonPath("$.unreadCount").value(0))
                .andExpect(jsonPath("$.items[0].unread").value(false));
    }

    @Test
    void readingIsPerMember() throws Exception {
        publish("Notifications on the avatar");

        mvc.perform(post("/api/notifications/read").with(as(owner))).andExpect(status().isNoContent());

        listFor(other).andExpect(jsonPath("$.unreadCount").value(1));
    }
}

package com.opslog;

import com.opslog.auth.UserAccount;
import com.opslog.auth.UserAccountRepository;
import com.opslog.location.Location;
import com.opslog.location.LocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CheckInApiTests {

    @Autowired MockMvc mvc;
    @Autowired UserAccountRepository users;
    @Autowired LocationRepository locations;

    private final LocalDate today = LocalDate.now(ZoneOffset.UTC);

    private UserAccount owner;
    private UserAccount other;
    private long downtown;

    @BeforeEach
    void setUp() {
        owner = users.save(new UserAccount("owner@example.com", "x".repeat(60), "Owner"));
        other = users.save(new UserAccount("other@example.com", "x".repeat(60), "Other"));
        downtown = locations.findVisibleTo(owner.getId()).stream()
                .filter(l -> l.getName().equals("Kaimana Downtown")).findFirst().orElseThrow().getId();
    }

    private RequestPostProcessor as(UserAccount user) {
        return jwt().jwt(token -> token.subject(user.getId().toString()));
    }

    private ResultActions checkIn(UserAccount user, LocalDate date, String body) throws Exception {
        return mvc.perform(put("/api/checkins/" + date).with(as(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String body(int sleep, int soreness, long locationId) {
        return """
                {"sleepQuality":%d,"soreness":%d,"locationId":%d}""".formatted(sleep, soreness, locationId);
    }

    @Test
    void tiersComeFromTheDatabase() throws Exception {
        mvc.perform(get("/api/tiers").with(as(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", contains("Recovery", "Reduced", "Full")))
                .andExpect(jsonPath("$[2].kind").value("FULL"))
                .andExpect(jsonPath("$[2].minSleep").value(4));
    }

    @Test
    void firstCheckInIsCreatedWithTheSelectedTier() throws Exception {
        checkIn(owner, today, body(5, 1, downtown))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.date").value(today.toString()))
                .andExpect(jsonPath("$.tier.name").value("Full"))
                .andExpect(jsonPath("$.location.name").value("Kaimana Downtown"));
    }

    @Test
    void resubmittingTheSameDayUpdatesItAndReselectsTheTier() throws Exception {
        checkIn(owner, today, body(5, 1, downtown)).andExpect(status().isCreated());
        checkIn(owner, today, body(2, 4, downtown))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tier.name").value("Recovery"));
        mvc.perform(get("/api/checkins/" + today).with(as(owner)))
                .andExpect(jsonPath("$.sleepQuality").value(2))
                .andExpect(jsonPath("$.tier.kind").value("RECOVERY"));
    }

    @Test
    void listReturnsMultipleDaysNewestFirst() throws Exception {
        checkIn(owner, today.minusDays(2), body(3, 3, downtown));
        checkIn(owner, today.minusDays(1), body(5, 2, downtown));
        checkIn(owner, today, body(1, 5, downtown));
        mvc.perform(get("/api/checkins").with(as(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tier.name", contains("Recovery", "Full", "Reduced")));
        mvc.perform(get("/api/checkins")
                        .param("from", today.minusDays(1).toString())
                        .param("to", today.minusDays(1).toString())
                        .with(as(owner)))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void checkInsArePerUser() throws Exception {
        checkIn(owner, today, body(5, 1, downtown));
        mvc.perform(get("/api/checkins/" + today).with(as(other)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No check-in for " + today));
        mvc.perform(get("/api/checkins").with(as(other)))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void anotherUsersLocationCannotBeUsed() throws Exception {
        long garage = locations.save(new Location("Garage", owner.getId())).getId();
        checkIn(owner, today, body(4, 2, garage)).andExpect(status().isCreated());
        checkIn(other, today, body(4, 2, garage)).andExpect(status().isNotFound());
    }

    @Test
    void invalidScoresReturn400WithFieldErrors() throws Exception {
        checkIn(owner, today, """
                {"sleepQuality":6,"soreness":0}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.sleepQuality").value("Sleep quality must be between 1 and 5"))
                .andExpect(jsonPath("$.errors.soreness").value("Soreness must be between 1 and 5"))
                .andExpect(jsonPath("$.errors.locationId").value("Location is required"));
    }

    @Test
    void futureDatesAndBadRangesReturn400() throws Exception {
        checkIn(owner, today.plusDays(2), body(5, 1, downtown))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Check-in date cannot be in the future"));
        mvc.perform(put("/api/checkins/not-a-date").with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(5, 1, downtown)))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/checkins")
                        .param("from", today.toString())
                        .param("to", today.minusDays(1).toString())
                        .with(as(owner)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("The start date must be on or before the end date"));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/checkins")).andExpect(status().isUnauthorized());
    }
}

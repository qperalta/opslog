package com.opslog;

import com.opslog.auth.UserAccount;
import com.opslog.auth.UserAccountRepository;
import com.opslog.location.Location;
import com.opslog.location.LocationRepository;
import com.opslog.plan.ProgramTemplate;
import com.opslog.plan.ProgramTemplateRepository;
import com.opslog.plan.TemplateDay;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
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
class SessionApiTests {

    @Autowired MockMvc mvc;
    @Autowired UserAccountRepository users;
    @Autowired LocationRepository locations;
    @Autowired ProgramTemplateRepository templates;

    /* check-ins cannot be in the future, so tests use today */
    private final LocalDate today = LocalDate.now(ZoneOffset.UTC);

    private UserAccount owner;
    private UserAccount other;
    private ProgramTemplate upperLower;
    private Location downtown;
    private Location kapolei;

    @BeforeEach
    void setUp() {
        owner = users.save(new UserAccount("owner@example.com", "x".repeat(60), "Owner"));
        other = users.save(new UserAccount("other@example.com", "x".repeat(60), "Other"));
        upperLower = templates.findAll().stream().filter(t -> t.getName().equals("Upper/Lower 4-Day")).findFirst().orElseThrow();
        downtown = location("Kaimana Downtown");
        kapolei = location("Kaimana Kapolei");
    }

    private Location location(String name) {
        return locations.findVisibleTo(owner.getId()).stream().filter(l -> l.getName().equals(name)).findFirst().orElseThrow();
    }

    private long workoutId(String name) {
        return upperLower.getDays().stream().filter(d -> d.getName().equals(name)).findFirst().map(TemplateDay::getId).orElseThrow();
    }

    private RequestPostProcessor as(UserAccount user) {
        return jwt().jwt(token -> token.subject(user.getId().toString()));
    }

    private void put(UserAccount user, String path, String body) throws Exception {
        mvc.perform(MockMvcRequestBuilders.put(path).with(as(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is2xxSuccessful());
    }

    /* program chosen and today set to Upper A, so the test does not depend on the weekday */
    private void upperAToday(UserAccount user) throws Exception {
        put(user, "/api/program", """
                {"templateId":%d}""".formatted(upperLower.getId()));
        put(user, "/api/plan/" + today, """
                {"mode":"TRAINING","workoutId":%d}""".formatted(workoutId("Upper A")));
    }

    private void checkIn(UserAccount user, int sleep, int soreness, Location location) throws Exception {
        put(user, "/api/checkins/" + today, """
                {"sleepQuality":%d,"soreness":%d,"locationId":%d}""".formatted(sleep, soreness, location.getId()));
    }

    private ResultActions session(UserAccount user) throws Exception {
        return mvc.perform(get("/api/sessions/" + today).with(as(user)));
    }

    private ResultActions rebuild(UserAccount user) throws Exception {
        return mvc.perform(post("/api/sessions/" + today + "/rebuild").with(as(user)));
    }

    @Test
    void sessionNeedsAToken() throws Exception {
        mvc.perform(get("/api/sessions/" + today)).andExpect(status().isUnauthorized());
    }

    @Test
    void sessionNeedsAProgram() throws Exception {
        session(owner)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No program has been chosen yet"));
    }

    @Test
    void restDayHasNoSession() throws Exception {
        put(owner, "/api/program", """
                {"templateId":%d}""".formatted(upperLower.getId()));
        put(owner, "/api/plan/" + today, """
                {"mode":"REST"}""");
        session(owner)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REST"))
                .andExpect(jsonPath("$.exercises", hasSize(0)));
    }

    @Test
    void trainingDayWaitsForTheCheckIn() throws Exception {
        upperAToday(owner);
        session(owner)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEEDS_CHECKIN"))
                .andExpect(jsonPath("$.workout.name").value("Upper A"))
                .andExpect(jsonPath("$.exercises", hasSize(0)));
    }

    @Test
    void fullTierAtAFullGymIsTheWorkoutAsWritten() throws Exception {
        upperAToday(owner);
        checkIn(owner, 5, 1, downtown);
        session(owner)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.source").value("RULES"))
                .andExpect(jsonPath("$.tier.name").value("Full"))
                .andExpect(jsonPath("$.tierKind").value("FULL"))
                .andExpect(jsonPath("$.location.name").value("Kaimana Downtown"))
                .andExpect(jsonPath("$.notes", contains("Full tier: The session as written")))
                .andExpect(jsonPath("$.exercises", hasSize(6)))
                .andExpect(jsonPath("$.exercises[0].name").value("Barbell Bench Press"))
                .andExpect(jsonPath("$.exercises[0].kind").value("STRENGTH"))
                .andExpect(jsonPath("$.exercises[0].target").value("5-8 reps"))
                .andExpect(jsonPath("$.exercises[0].sets").value(4))
                .andExpect(jsonPath("$.exercises[0].mainLift").value(true))
                .andExpect(jsonPath("$.exercises[*].substitutedFor", everyItem(nullValue())));
    }

    @Test
    void dumbbellGymSwapsTheBarbellLifts() throws Exception {
        upperAToday(owner);
        checkIn(owner, 5, 1, kapolei);
        session(owner)
                .andExpect(jsonPath("$.exercises[0].name").value("Dumbbell Floor Press"))
                .andExpect(jsonPath("$.exercises[0].substitutedFor.name").value("Barbell Bench Press"))
                .andExpect(jsonPath("$.exercises[0].reason").value("No Barbell, Flat Bench at Kaimana Kapolei"))
                .andExpect(jsonPath("$.exercises[1].name").value("Chest-Supported Dumbbell Row"))
                .andExpect(jsonPath("$.notes", hasItem("Barbell Bench Press -> Dumbbell Floor Press")));
    }

    @Test
    void reducedTierKeepsMainLiftsAndCutsAccessories() throws Exception {
        upperAToday(owner);
        checkIn(owner, 3, 3, downtown);
        session(owner)
                .andExpect(jsonPath("$.tier.name").value("Reduced"))
                .andExpect(jsonPath("$.exercises[0].sets").value(4))
                .andExpect(jsonPath("$.exercises[2].name").value("Seated Dumbbell Shoulder Press"))
                .andExpect(jsonPath("$.exercises[2].sets").value(2))
                .andExpect(jsonPath("$.exercises[2].reason").value("3 -> 2 sets"));
    }

    @Test
    void checkInLocationWinsOverThePlannedLocation() throws Exception {
        upperAToday(owner);
        put(owner, "/api/plan/" + today, """
                {"mode":"TRAINING","workoutId":%d,"locationId":%d}""".formatted(workoutId("Upper A"), downtown.getId()));
        checkIn(owner, 5, 1, kapolei);
        session(owner).andExpect(jsonPath("$.location.name").value("Kaimana Kapolei"));
    }

    @Test
    void storedSessionIsReturnedUntilRebuilt() throws Exception {
        upperAToday(owner);
        checkIn(owner, 5, 1, downtown);
        session(owner).andExpect(jsonPath("$.tier.name").value("Full"));

        checkIn(owner, 1, 5, downtown);
        session(owner).andExpect(jsonPath("$.tier.name").value("Full"));

        rebuild(owner)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tier.name").value("Recovery"))
                .andExpect(jsonPath("$.exercises[0].sets").value(2));
        session(owner).andExpect(jsonPath("$.tier.name").value("Recovery"));
    }

    @Test
    void rebuildOnARestDayClearsTheSession() throws Exception {
        upperAToday(owner);
        checkIn(owner, 5, 1, downtown);
        session(owner).andExpect(jsonPath("$.status").value("READY"));

        put(owner, "/api/plan/" + today, """
                {"mode":"REST"}""");
        rebuild(owner).andExpect(jsonPath("$.status").value("REST"));
        session(owner).andExpect(jsonPath("$.status").value("REST"));
    }

    @Test
    void sessionsAreScopedToTheUser() throws Exception {
        upperAToday(owner);
        checkIn(owner, 5, 1, downtown);
        session(owner).andExpect(jsonPath("$.status").value("READY"));

        upperAToday(other);
        session(other)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEEDS_CHECKIN"));
    }
}

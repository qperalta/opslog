package com.opslog;

import com.opslog.auth.UserAccount;
import com.opslog.auth.UserAccountRepository;
import com.opslog.location.Location;
import com.opslog.location.LocationRepository;
import com.opslog.plan.ProgramTemplate;
import com.opslog.plan.ProgramTemplateRepository;
import com.opslog.plan.TemplateDay;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ExerciseTimingApiTests {

    @Autowired MockMvc mvc;
    @Autowired UserAccountRepository users;
    @Autowired LocationRepository locations;
    @Autowired ProgramTemplateRepository templates;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;

    private final LocalDate today = LocalDate.now(ZoneOffset.UTC);

    private UserAccount owner;
    private UserAccount other;
    private ProgramTemplate upperLower;
    private Location downtown;

    @BeforeEach
    void setUp() {
        owner = users.save(new UserAccount("owner@example.com", "x".repeat(60), "Owner"));
        other = users.save(new UserAccount("other@example.com", "x".repeat(60), "Other"));
        upperLower = templates.findAll().stream().filter(t -> t.getName().equals("Upper/Lower 4-Day")).findFirst().orElseThrow();
        downtown = locations.findVisibleTo(owner.getId()).stream()
                .filter(l -> l.getName().equals("Kaimana Downtown")).findFirst().orElseThrow();
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

    /* Upper A at a full gym; Barbell Bench Press is position 1 */
    private ResultActions sessionToday(UserAccount user) throws Exception {
        long workoutId = upperLower.getDays().stream().filter(d -> d.getName().equals("Upper A"))
                .findFirst().map(TemplateDay::getId).orElseThrow();
        put(user, "/api/program", """
                {"templateId":%d}""".formatted(upperLower.getId()));
        put(user, "/api/plan/" + today, """
                {"mode":"TRAINING","workoutId":%d}""".formatted(workoutId));
        put(user, "/api/checkins/" + today, """
                {"sleepQuality":5,"soreness":1,"locationId":%d}""".formatted(downtown.getId()));
        return mvc.perform(get("/api/sessions/" + today).with(as(user)));
    }

    private ResultActions setTiming(UserAccount user, long exerciseId, String body) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.put("/api/exercise-timings/" + exerciseId).with(as(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private long exerciseId(String name) {
        return jdbc.queryForObject("SELECT id FROM exercises WHERE name = ? AND owner_id IS NULL", Long.class, name);
    }

    /* flushes first: plain SQL does not see what Hibernate has not written yet */
    private long savedTimings(UserAccount user) {
        entityManager.flush();
        return jdbc.queryForObject("SELECT count(*) FROM exercise_timings WHERE user_id = ?", Long.class, user.getId());
    }

    @Test
    void timingNeedsAToken() throws Exception {
        mvc.perform(MockMvcRequestBuilders.put("/api/exercise-timings/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"restSeconds\":45}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void restTimeShowsOnTheSession() throws Exception {
        setTiming(owner, exerciseId("Barbell Bench Press"), """
                {"restSeconds":120}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restSeconds").value(120))
                .andExpect(jsonPath("$.holdSeconds").value(nullValue()));

        sessionToday(owner)
                .andExpect(jsonPath("$.exercises[0].restSeconds").value(120))
                .andExpect(jsonPath("$.exercises[0].holdSeconds").value(nullValue()))
                .andExpect(jsonPath("$.exercises[1].restSeconds").value(nullValue()));
    }

    @Test
    void timedExerciseTakesAHoldTime() throws Exception {
        setTiming(owner, exerciseId("Plank"), """
                {"holdSeconds":75,"restSeconds":45}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holdSeconds").value(75))
                .andExpect(jsonPath("$.restSeconds").value(45));
    }

    @Test
    void strengthExerciseHasNoHoldTimer() throws Exception {
        setTiming(owner, exerciseId("Barbell Bench Press"), """
                {"holdSeconds":60}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Only timed exercises have a hold timer"));
        assertEquals(0, savedTimings(owner));
    }

    @Test
    void outOfRangeTimesAreRefused() throws Exception {
        setTiming(owner, exerciseId("Plank"), """
                {"holdSeconds":601}""")
                .andExpect(status().isBadRequest());
        setTiming(owner, exerciseId("Plank"), """
                {"restSeconds":-1}""")
                .andExpect(status().isBadRequest());
    }

    @Test
    void clearingBothGoesBackToTheDefaults() throws Exception {
        setTiming(owner, exerciseId("Plank"), """
                {"holdSeconds":75,"restSeconds":45}""");

        setTiming(owner, exerciseId("Plank"), "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holdSeconds").value(nullValue()));

        assertEquals(0, savedTimings(owner));
    }

    @Test
    void timesArePerMember() throws Exception {
        setTiming(owner, exerciseId("Barbell Bench Press"), """
                {"restSeconds":120}""");

        sessionToday(other).andExpect(jsonPath("$.exercises[0].restSeconds").value(nullValue()));
    }

    @Test
    void anotherMembersExerciseIsNotFound() throws Exception {
        long pattern = jdbc.queryForObject("SELECT id FROM movement_patterns ORDER BY id LIMIT 1", Long.class);
        long muscle = jdbc.queryForObject("SELECT id FROM muscle_groups ORDER BY id LIMIT 1", Long.class);
        String body = mvc.perform(post("/api/catalog/exercises").with(as(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"kind":"TIMED","name":"Copenhagen Plank","movementPatternId":%d,"primaryMuscleId":%d,"targetSeconds":30}"""
                                .formatted(pattern, muscle)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long theirs = ((Number) com.jayway.jsonpath.JsonPath.read(body, "$.id")).longValue();

        setTiming(owner, theirs, """
                {"restSeconds":45}""")
                .andExpect(status().isNotFound());
    }

    @Test
    void timesOnTheAddCardAreSaved() throws Exception {
        sessionToday(owner);

        mvc.perform(post("/api/sessions/" + today + "/exercises").with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"position":1,"exerciseId":%d,"sets":3,"holdSeconds":75,"restSeconds":45}""".formatted(exerciseId("Plank"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exercises[0].holdSeconds").value(75))
                .andExpect(jsonPath("$.exercises[0].restSeconds").value(45));
    }
}

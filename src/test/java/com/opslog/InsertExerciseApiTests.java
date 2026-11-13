package com.opslog;

import com.jayway.jsonpath.JsonPath;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InsertExerciseApiTests {

    @Autowired MockMvc mvc;
    @Autowired UserAccountRepository users;
    @Autowired LocationRepository locations;
    @Autowired ProgramTemplateRepository templates;
    @Autowired JdbcTemplate jdbc;

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

    /* full tier at a full gym; returns the exercise names in order */
    private List<String> sessionToday(UserAccount user) throws Exception {
        long workoutId = upperLower.getDays().stream().filter(d -> d.getName().equals("Upper A"))
                .findFirst().map(TemplateDay::getId).orElseThrow();
        put(user, "/api/program", """
                {"templateId":%d}""".formatted(upperLower.getId()));
        put(user, "/api/plan/" + today, """
                {"mode":"TRAINING","workoutId":%d}""".formatted(workoutId));
        put(user, "/api/checkins/" + today, """
                {"sleepQuality":5,"soreness":1,"locationId":%d}""".formatted(downtown.getId()));
        String body = mvc.perform(get("/api/sessions/" + today).with(as(user)))
                .andExpect(jsonPath("$.status").value("READY"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.exercises[*].name");
    }

    private ResultActions insert(UserAccount user, String body) throws Exception {
        return mvc.perform(post("/api/sessions/" + today + "/exercises").with(as(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private long exerciseId(String name) {
        return jdbc.queryForObject("SELECT id FROM exercises WHERE name = ? AND owner_id IS NULL", Long.class, name);
    }

    /* the position check is deferred to commit, and these tests roll back, so check it now */
    private void positionsStayUnique() {
        jdbc.execute("SET CONSTRAINTS ALL IMMEDIATE");
    }

    private String newExercise(String name) {
        long pattern = jdbc.queryForObject("SELECT id FROM movement_patterns ORDER BY id LIMIT 1", Long.class);
        long muscle = jdbc.queryForObject("SELECT id FROM muscle_groups ORDER BY id LIMIT 1", Long.class);
        return """
                {"kind":"STRENGTH","name":"%s","movementPatternId":%d,"primaryMuscleId":%d,"repMin":10,"repMax":10}"""
                .formatted(name, pattern, muscle);
    }

    @Test
    void insertingNeedsAToken() throws Exception {
        mvc.perform(post("/api/sessions/" + today + "/exercises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void insertInTheMiddleMovesTheRestDown() throws Exception {
        List<String> before = sessionToday(owner);

        insert(owner, """
                {"position":2,"exerciseId":%d,"sets":3,"targetLoadLb":25}""".formatted(exerciseId("Face Pull")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exercises.length()").value(before.size() + 1))
                .andExpect(jsonPath("$.exercises[0].name").value(before.get(0)))
                .andExpect(jsonPath("$.exercises[1].name").value("Face Pull"))
                .andExpect(jsonPath("$.exercises[1].position").value(2))
                .andExpect(jsonPath("$.exercises[1].adHoc").value(true))
                .andExpect(jsonPath("$.exercises[1].sets").value(3))
                .andExpect(jsonPath("$.exercises[1].targetLoadLb").value(25))
                .andExpect(jsonPath("$.exercises[2].name").value(before.get(1)))
                .andExpect(jsonPath("$.exercises[2].position").value(3))
                .andExpect(jsonPath("$.exercises[2].adHoc").value(false));
        positionsStayUnique();
    }

    @Test
    void setsLogToTheExerciseAtItsNewPosition() throws Exception {
        List<String> before = sessionToday(owner);
        insert(owner, """
                {"position":1,"exerciseId":%d,"sets":2}""".formatted(exerciseId("Face Pull")))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/sessions/" + today + "/exercises/2/sets").with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reps":8,"loadLb":135}"""))
                .andExpect(status().isCreated());
        positionsStayUnique();

        mvc.perform(get("/api/sessions/" + today).with(as(owner)))
                .andExpect(jsonPath("$.exercises[1].name").value(before.getFirst()))
                .andExpect(jsonPath("$.exercises[1].logged.length()").value(1))
                .andExpect(jsonPath("$.exercises[0].logged.length()").value(0));
    }

    @Test
    void appendAfterTheLast() throws Exception {
        int count = sessionToday(owner).size();

        insert(owner, """
                {"position":%d,"exerciseId":%d,"sets":1}""".formatted(count + 1, exerciseId("Plank")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exercises[%d].name".formatted(count)).value("Plank"));
    }

    @Test
    void newNameBecomesTheMembersOwnExercise() throws Exception {
        sessionToday(owner);

        String body = insert(owner, """
                {"position":1,"newExercise":%s,"sets":3,"targetLoadLb":40}""".formatted(newExercise("  Sled Push  ")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exercises[0].name").value("Sled Push"))
                .andExpect(jsonPath("$.exercises[0].adHoc").value(true))
                .andReturn().getResponse().getContentAsString();
        positionsStayUnique();

        long id = ((Number) JsonPath.read(body, "$.exercises[0].exerciseId")).longValue();
        mvc.perform(get("/api/catalog/exercises/" + id).with(as(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.custom").value(true));
        mvc.perform(get("/api/catalog/exercises/" + id).with(as(other)))
                .andExpect(status().isNotFound());
    }

    @Test
    void aNameAlreadyInTheListIsReused() throws Exception {
        sessionToday(owner);
        long custom = jdbc.queryForObject("SELECT count(*) FROM exercises WHERE owner_id = ?", Long.class, owner.getId());

        insert(owner, """
                {"position":1,"newExercise":%s,"sets":3}""".formatted(newExercise("face pull")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exercises[0].exerciseId").value(exerciseId("Face Pull")));

        assertEquals(custom, jdbc.queryForObject("SELECT count(*) FROM exercises WHERE owner_id = ?", Long.class, owner.getId()));
    }

    @Test
    void pickOneOrNameOneNotBoth() throws Exception {
        sessionToday(owner);

        insert(owner, """
                {"position":1,"exerciseId":%d,"newExercise":%s,"sets":3}""".formatted(exerciseId("Plank"), newExercise("Sled Push")))
                .andExpect(status().isBadRequest());
        insert(owner, """
                {"position":1,"sets":3}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Pick an exercise from the list or name a new one, not both"));
    }

    @Test
    void positionPastTheEndIsRefused() throws Exception {
        int count = sessionToday(owner).size();

        insert(owner, """
                {"position":%d,"exerciseId":%d,"sets":3}""".formatted(count + 2, exerciseId("Plank")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Position must be between 1 and " + (count + 1)));
    }

    @Test
    void setsAreRequired() throws Exception {
        sessionToday(owner);

        insert(owner, """
                {"position":1,"exerciseId":%d}""".formatted(exerciseId("Plank")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anotherMembersExerciseCannotBeUsed() throws Exception {
        sessionToday(owner);
        sessionToday(other);
        String body = insert(other, """
                {"position":1,"newExercise":%s,"sets":3}""".formatted(newExercise("Sled Push")))
                .andReturn().getResponse().getContentAsString();
        long theirs = ((Number) JsonPath.read(body, "$.exercises[0].exerciseId")).longValue();

        insert(owner, """
                {"position":1,"exerciseId":%d,"sets":3}""".formatted(theirs))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/sessions/" + today).with(as(owner)))
                .andExpect(jsonPath("$.exercises[*].name", org.hamcrest.Matchers.not(hasItem("Sled Push"))));
    }

    @Test
    void noSessionYetIsRefused() throws Exception {
        insert(owner, """
                {"position":1,"exerciseId":%d,"sets":3}""".formatted(exerciseId("Plank")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("There is no session for " + today + " yet"));
    }
}

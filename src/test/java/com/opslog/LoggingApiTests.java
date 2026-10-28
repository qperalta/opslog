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
class LoggingApiTests {

    @Autowired MockMvc mvc;
    @Autowired UserAccountRepository users;
    @Autowired LocationRepository locations;
    @Autowired ProgramTemplateRepository templates;

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

    /* full tier at a full gym, so the session is the workout as written */
    private void sessionToday(UserAccount user, String workout) throws Exception {
        put(user, "/api/program", """
                {"templateId":%d}""".formatted(upperLower.getId()));
        put(user, "/api/plan/" + today, """
                {"mode":"TRAINING","workoutId":%d}""".formatted(workoutId(workout)));
        put(user, "/api/checkins/" + today, """
                {"sleepQuality":5,"soreness":1,"locationId":%d}""".formatted(downtown.getId()));
        mvc.perform(get("/api/sessions/" + today).with(as(user))).andExpect(jsonPath("$.status").value("READY"));
    }

    private String sets(int position) {
        return "/api/sessions/" + today + "/exercises/" + position + "/sets";
    }

    private ResultActions log(UserAccount user, int position, String body) throws Exception {
        return mvc.perform(post(sets(position)).with(as(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions update(UserAccount user, int position, int setNumber, String body) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.put(sets(position) + "/" + setNumber).with(as(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions remove(UserAccount user, int position, int setNumber) throws Exception {
        return mvc.perform(delete(sets(position) + "/" + setNumber).with(as(user)));
    }

    private ResultActions session(UserAccount user) throws Exception {
        return mvc.perform(get("/api/sessions/" + today).with(as(user)));
    }

    @Test
    void loggingNeedsAToken() throws Exception {
        mvc.perform(post(sets(1)).contentType(MediaType.APPLICATION_JSON).content("{\"reps\":5}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loggingNeedsAStoredSession() throws Exception {
        log(owner, 1, """
                {"reps":5,"loadLb":135}""")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No exercise 1 in the session for " + today));
    }

    @Test
    void strengthSetIsLoggedAndShownOnTheSession() throws Exception {
        sessionToday(owner, "Upper A");
        log(owner, 1, """
                {"reps":8,"loadLb":135}""")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", sets(1) + "/1"))
                .andExpect(jsonPath("$.setNumber").value(1))
                .andExpect(jsonPath("$.reps").value(8))
                .andExpect(jsonPath("$.loadLb").value(135))
                .andExpect(jsonPath("$.durationSeconds").value(nullValue()))
                .andExpect(jsonPath("$.clean").value(true))
                .andExpect(jsonPath("$.loggedAt").exists());

        session(owner)
                .andExpect(jsonPath("$.exercises[0].logged", hasSize(1)))
                .andExpect(jsonPath("$.exercises[0].logged[0].reps").value(8))
                .andExpect(jsonPath("$.exercises[1].logged", hasSize(0)));
    }

    @Test
    void setsAreNumberedInOrder() throws Exception {
        sessionToday(owner, "Upper A");
        log(owner, 1, "{\"reps\":8,\"loadLb\":135}").andExpect(jsonPath("$.setNumber").value(1));
        log(owner, 1, "{\"reps\":7,\"loadLb\":135,\"clean\":false}")
                .andExpect(jsonPath("$.setNumber").value(2))
                .andExpect(jsonPath("$.clean").value(false));
        session(owner).andExpect(jsonPath("$.exercises[0].logged[*].setNumber", contains(1, 2)));
    }

    @Test
    void strengthSetNeedsReps() throws Exception {
        sessionToday(owner, "Upper A");
        log(owner, 1, "{\"loadLb\":135}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Reps are required for a strength set"));
        log(owner, 1, "{\"reps\":5,\"durationSeconds\":30}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("A strength set logs reps, not seconds"));
    }

    @Test
    void timedSetLogsSeconds() throws Exception {
        sessionToday(owner, "Lower A");
        log(owner, 6, "{\"durationSeconds\":45}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.durationSeconds").value(45))
                .andExpect(jsonPath("$.reps").value(nullValue()));
        log(owner, 6, "{\"reps\":10}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Seconds are required for a timed or cardio set"));
    }

    @Test
    void invalidFieldsAreRejected() throws Exception {
        sessionToday(owner, "Upper A");
        log(owner, 1, "{\"reps\":101,\"loadLb\":-5}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.reps").value("Reps must be between 0 and 100"))
                .andExpect(jsonPath("$.errors.loadLb").value("Load cannot be negative"));
        log(owner, 1, "{\"reps\":5,\"loadLb\":135.255}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.loadLb").value("Load must be under 10000 lb with at most 2 decimals"));
    }

    @Test
    void anExerciseCanHaveAtMostTenSets() throws Exception {
        sessionToday(owner, "Upper A");
        for(int i = 1; i <= 10; i++) log(owner, 5, "{\"reps\":10}").andExpect(status().isCreated());
        log(owner, 5, "{\"reps\":10}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("An exercise can have at most 10 logged sets"));
    }

    @Test
    void positionMustBeInTheSession() throws Exception {
        sessionToday(owner, "Upper A");
        log(owner, 7, "{\"reps\":5}").andExpect(status().isNotFound());
    }

    @Test
    void setCanBeEdited() throws Exception {
        sessionToday(owner, "Upper A");
        log(owner, 1, "{\"reps\":8,\"loadLb\":135}");
        update(owner, 1, 1, "{\"reps\":6,\"loadLb\":145,\"clean\":false}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.setNumber").value(1))
                .andExpect(jsonPath("$.reps").value(6))
                .andExpect(jsonPath("$.loadLb").value(145))
                .andExpect(jsonPath("$.clean").value(false));
        session(owner).andExpect(jsonPath("$.exercises[0].logged[0].reps").value(6));
        update(owner, 1, 2, "{\"reps\":6}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Set 2 was not found"));
    }

    @Test
    void setCanBeDeleted() throws Exception {
        sessionToday(owner, "Upper A");
        log(owner, 1, "{\"reps\":8,\"loadLb\":135}");
        remove(owner, 1, 1).andExpect(status().isNoContent());
        session(owner).andExpect(jsonPath("$.exercises[0].logged", hasSize(0)));
        remove(owner, 1, 1).andExpect(status().isNotFound());
    }

    @Test
    void anotherUserCannotLogToTheSession() throws Exception {
        sessionToday(owner, "Upper A");
        log(owner, 1, "{\"reps\":8,\"loadLb\":135}");
        log(other, 1, "{\"reps\":8}").andExpect(status().isNotFound());
        update(other, 1, 1, "{\"reps\":1}").andExpect(status().isNotFound());
        remove(other, 1, 1).andExpect(status().isNotFound());
        session(owner).andExpect(jsonPath("$.exercises[0].logged[0].reps").value(8));
    }

    @Test
    void rebuildIsRefusedOnceSetsAreLogged() throws Exception {
        sessionToday(owner, "Upper A");
        log(owner, 1, "{\"reps\":8,\"loadLb\":135}");
        mvc.perform(post("/api/sessions/" + today + "/rebuild").with(as(owner)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("The session for " + today + " already has logged sets and cannot be rebuilt"));

        remove(owner, 1, 1);
        mvc.perform(post("/api/sessions/" + today + "/rebuild").with(as(owner))).andExpect(status().isOk());
    }
}

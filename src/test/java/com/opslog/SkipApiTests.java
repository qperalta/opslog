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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SkipApiTests {

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

    private RequestPostProcessor as(UserAccount user) {
        return jwt().jwt(token -> token.subject(user.getId().toString()));
    }

    private void put(UserAccount user, String path, String body) throws Exception {
        mvc.perform(MockMvcRequestBuilders.put(path).with(as(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is2xxSuccessful());
    }

    /* full tier at a full gym; returns how many exercises the session has */
    private int sessionToday(UserAccount user) throws Exception {
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
        return JsonPath.read(body, "$.exercises.length()");
    }

    private ResultActions skip(UserAccount user, int position, String body) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.put("/api/sessions/" + today + "/exercises/" + position + "/skipped")
                .with(as(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions log(UserAccount user, int position) throws Exception {
        return mvc.perform(post("/api/sessions/" + today + "/exercises/" + position + "/sets").with(as(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"reps":8,"loadLb":135}"""));
    }

    @Test
    void skippingNeedsAToken() throws Exception {
        mvc.perform(MockMvcRequestBuilders.put("/api/sessions/" + today + "/exercises/1/skipped")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skipped\":true}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void skippedIsRequired() throws Exception {
        sessionToday(owner);
        skip(owner, 1, "{}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void skippedExerciseShowsOnTheSessionAndTakesNoSets() throws Exception {
        sessionToday(owner);
        skip(owner, 2, """
                {"skipped":true}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value(2))
                .andExpect(jsonPath("$.skipped").value(true));

        mvc.perform(get("/api/sessions/" + today).with(as(owner)))
                .andExpect(jsonPath("$.exercises[0].skipped").value(false))
                .andExpect(jsonPath("$.exercises[1].skipped").value(true));

        log(owner, 2)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Re-activate this exercise before logging a set"));
    }

    @Test
    void reactivatedExerciseTakesSetsAgain() throws Exception {
        sessionToday(owner);
        skip(owner, 2, """
                {"skipped":true}""");
        skip(owner, 2, """
                {"skipped":false}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skipped").value(false));

        log(owner, 2).andExpect(status().isCreated());
    }

    @Test
    void exerciseWithLoggedSetsCannotBeSkipped() throws Exception {
        sessionToday(owner);
        log(owner, 1).andExpect(status().isCreated());

        skip(owner, 1, """
                {"skipped":true}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Delete the logged sets before skipping this exercise"));
    }

    @Test
    void anotherUsersSessionIsNotFound() throws Exception {
        sessionToday(owner);
        skip(other, 1, """
                {"skipped":true}""")
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/sessions/" + today).with(as(owner)))
                .andExpect(jsonPath("$.exercises[0].skipped").value(false));
    }

    @Test
    void trainingReportLeavesSkippedExercisesOutOfThePlan() throws Exception {
        int exercises = sessionToday(owner);
        skip(owner, 2, """
                {"skipped":true}""");
        log(owner, 1);

        mvc.perform(get("/api/reports/training?from=" + today + "&to=" + today).with(as(owner)))
                .andExpect(jsonPath("$.rows[0].exercisesPlanned").value(exercises - 1))
                .andExpect(jsonPath("$.rows[0].exercisesDone").value(1));
    }
}

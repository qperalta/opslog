package com.opslog;

import com.opslog.auth.UserAccount;
import com.opslog.auth.UserAccountRepository;
import com.opslog.catalog.MuscleGroupRepository;
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

/* yesterday: 2 bench sets and 1 row set (Upper A); today: 2 squat sets and 1 plank (Lower A) */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HistoryApiTests {

    @Autowired MockMvc mvc;
    @Autowired UserAccountRepository users;
    @Autowired LocationRepository locations;
    @Autowired ProgramTemplateRepository templates;
    @Autowired MuscleGroupRepository muscles;

    private final LocalDate today = LocalDate.now(ZoneOffset.UTC);
    private final LocalDate yesterday = today.minusDays(1);

    private UserAccount owner;
    private UserAccount other;
    private ProgramTemplate upperLower;
    private Location downtown;

    @BeforeEach
    void setUp() throws Exception {
        owner = users.save(new UserAccount("owner@example.com", "x".repeat(60), "Owner"));
        other = users.save(new UserAccount("other@example.com", "x".repeat(60), "Other"));
        upperLower = templates.findAll().stream().filter(t -> t.getName().equals("Upper/Lower 4-Day")).findFirst().orElseThrow();
        downtown = locations.findVisibleTo(owner.getId()).stream()
                .filter(l -> l.getName().equals("Kaimana Downtown")).findFirst().orElseThrow();

        session(owner, yesterday, "Upper A");
        logSet(owner, yesterday, 1, "{\"reps\":8,\"loadLb\":135}");
        logSet(owner, yesterday, 1, "{\"reps\":7,\"loadLb\":135,\"clean\":false}");
        logSet(owner, yesterday, 2, "{\"reps\":10,\"loadLb\":115}");
        session(owner, today, "Lower A");
        logSet(owner, today, 1, "{\"reps\":5,\"loadLb\":185}");
        logSet(owner, today, 1, "{\"reps\":5,\"loadLb\":185}");
        logSet(owner, today, 6, "{\"durationSeconds\":45}");
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

    private void session(UserAccount user, LocalDate date, String workout) throws Exception {
        put(user, "/api/program", """
                {"templateId":%d}""".formatted(upperLower.getId()));
        put(user, "/api/plan/" + date, """
                {"mode":"TRAINING","workoutId":%d}""".formatted(workoutId(workout)));
        put(user, "/api/checkins/" + date, """
                {"sleepQuality":5,"soreness":1,"locationId":%d}""".formatted(downtown.getId()));
        mvc.perform(get("/api/sessions/" + date).with(as(user))).andExpect(jsonPath("$.status").value("READY"));
    }

    private void logSet(UserAccount user, LocalDate date, int position, String body) throws Exception {
        mvc.perform(post("/api/sessions/" + date + "/exercises/" + position + "/sets").with(as(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    private ResultActions history(UserAccount user, String query) throws Exception {
        return mvc.perform(get("/api/history" + query).with(as(user)));
    }

    @Test
    void historyNeedsAToken() throws Exception {
        mvc.perform(get("/api/history")).andExpect(status().isUnauthorized());
    }

    @Test
    void everySetIsARowNewestDayFirst() throws Exception {
        history(owner, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.items[*].date", contains(today.toString(), today.toString(), today.toString(),
                        yesterday.toString(), yesterday.toString(), yesterday.toString())))
                .andExpect(jsonPath("$.items[*].exercise", contains("Back Squat", "Back Squat", "Plank",
                        "Barbell Bench Press", "Barbell Bench Press", "Barbell Row")))
                .andExpect(jsonPath("$.items[0].setNumber").value(1))
                .andExpect(jsonPath("$.items[0].workout").value("Lower A"))
                .andExpect(jsonPath("$.items[0].location").value("Kaimana Downtown"))
                .andExpect(jsonPath("$.items[0].kind").value("STRENGTH"))
                .andExpect(jsonPath("$.items[0].primaryMuscle").value("Quadriceps"))
                .andExpect(jsonPath("$.items[0].loadLb").value(185))
                .andExpect(jsonPath("$.items[2].kind").value("TIMED"))
                .andExpect(jsonPath("$.items[2].durationSeconds").value(45))
                .andExpect(jsonPath("$.items[2].reps").value(nullValue()))
                .andExpect(jsonPath("$.items[4].clean").value(false));
    }

    @Test
    void filterByExercise() throws Exception {
        String benchId = history(owner, "").andReturn().getResponse().getContentAsString()
                .replaceAll("(?s).*\"exerciseId\":(\\d+),\"exercise\":\"Barbell Bench Press\".*", "$1");
        history(owner, "?exerciseId=" + benchId)
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items[*].exercise", everyItem(is("Barbell Bench Press"))))
                .andExpect(jsonPath("$.items[*].setNumber", contains(1, 2)));
    }

    @Test
    void filterByMuscleGroup() throws Exception {
        long chest = muscles.findAll().stream().filter(m -> m.getName().equals("Chest")).findFirst().orElseThrow().getId();
        history(owner, "?muscleGroupId=" + chest)
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items[*].primaryMuscle", everyItem(is("Chest"))));
    }

    @Test
    void filterByDateRange() throws Exception {
        history(owner, "?from=" + yesterday + "&to=" + yesterday)
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.items[*].date", everyItem(is(yesterday.toString()))));
        history(owner, "?from=" + today)
                .andExpect(jsonPath("$.totalElements").value(3));
        history(owner, "?to=" + yesterday.minusDays(1))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void startAfterEndIsRejected() throws Exception {
        history(owner, "?from=" + today + "&to=" + yesterday)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("The start date must be on or before the end date"));
    }

    @Test
    void resultsArePaged() throws Exception {
        history(owner, "?size=4&page=1")
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(4))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.items[*].exercise", contains("Barbell Bench Press", "Barbell Row")));
    }

    @Test
    void pageSizeIsLimited() throws Exception {
        history(owner, "?size=101").andExpect(status().isBadRequest());
    }

    @Test
    void historyIsScopedToTheUser() throws Exception {
        history(other, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}

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

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/* yesterday's logged sets set today's target loads */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProgressionApiTests {

    @Autowired MockMvc mvc;
    @Autowired UserAccountRepository users;
    @Autowired LocationRepository locations;
    @Autowired ProgramTemplateRepository templates;

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
        put(owner, "/api/program", """
                {"templateId":%d}""".formatted(upperLower.getId()));
        put(other, "/api/program", """
                {"templateId":%d}""".formatted(upperLower.getId()));
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

    private ResultActions session(UserAccount user, LocalDate date, int sleep, int soreness) throws Exception {
        put(user, "/api/plan/" + date, """
                {"mode":"TRAINING","workoutId":%d}""".formatted(workoutId("Upper A")));
        put(user, "/api/checkins/" + date, """
                {"sleepQuality":%d,"soreness":%d,"locationId":%d}""".formatted(sleep, soreness, downtown.getId()));
        return mvc.perform(get("/api/sessions/" + date).with(as(user)));
    }

    /* bench press is position 1 of Upper A: 4 sets of 5-8 */
    private void benchSets(UserAccount user, LocalDate date, int sets, int reps, boolean clean) throws Exception {
        for(int i = 0; i < sets; i++) {
            mvc.perform(post("/api/sessions/" + date + "/exercises/1/sets").with(as(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"reps":%d,"loadLb":135,"clean":%b}""".formatted(reps, clean)))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    void noHistoryMeansNoTargetLoad() throws Exception {
        session(owner, today, 5, 1).andExpect(jsonPath("$.exercises[0].targetLoadLb").value(nullValue()));
    }

    @Test
    void everySetAtTheTopOfTheRangeAddsFivePounds() throws Exception {
        session(owner, yesterday, 5, 1);
        benchSets(owner, yesterday, 4, 8, true);
        session(owner, today, 5, 1)
                .andExpect(jsonPath("$.exercises[0].name").value("Barbell Bench Press"))
                .andExpect(jsonPath("$.exercises[0].targetLoadLb").value(140.0))
                .andExpect(jsonPath("$.exercises[1].targetLoadLb").value(nullValue()));
    }

    @Test
    void aMissedRepKeepsTheLoad() throws Exception {
        session(owner, yesterday, 5, 1);
        benchSets(owner, yesterday, 4, 7, true);
        session(owner, today, 5, 1).andExpect(jsonPath("$.exercises[0].targetLoadLb").value(135.0));
    }

    @Test
    void recoveryTodayScalesTheProgressedLoad() throws Exception {
        session(owner, yesterday, 5, 1);
        benchSets(owner, yesterday, 4, 8, true);
        session(owner, today, 1, 5)
                .andExpect(jsonPath("$.tier.name").value("Recovery"))
                .andExpect(jsonPath("$.exercises[0].targetLoadLb").value(112.0));
    }

    @Test
    void recoveryDaysAreNotHistory() throws Exception {
        session(owner, yesterday, 1, 5);
        benchSets(owner, yesterday, 2, 8, true);
        session(owner, today, 5, 1).andExpect(jsonPath("$.exercises[0].targetLoadLb").value(nullValue()));
    }

    @Test
    void anotherUsersLogsDoNotCount() throws Exception {
        session(other, yesterday, 5, 1);
        benchSets(other, yesterday, 4, 8, true);
        session(owner, today, 5, 1).andExpect(jsonPath("$.exercises[0].targetLoadLb").value(nullValue()));
    }
}

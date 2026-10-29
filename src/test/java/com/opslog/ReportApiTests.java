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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/* Upper A: bench press (chest, push) is position 1, barbell row (upper back, pull) is position 2 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReportApiTests {

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
        for(UserAccount user : new UserAccount[] {owner, other}) {
            put(user, "/api/program", """
                    {"templateId":%d}""".formatted(upperLower.getId()));
        }
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

    private void session(UserAccount user, LocalDate date) throws Exception {
        long upperA = upperLower.getDays().stream().filter(d -> d.getName().equals("Upper A")).findFirst()
                .map(TemplateDay::getId).orElseThrow();
        put(user, "/api/plan/" + date, """
                {"mode":"TRAINING","workoutId":%d}""".formatted(upperA));
        put(user, "/api/checkins/" + date, """
                {"sleepQuality":5,"soreness":1,"locationId":%d}""".formatted(downtown.getId()));
        mvc.perform(get("/api/sessions/" + date).with(as(user))).andExpect(status().isOk());
    }

    private void logSet(UserAccount user, LocalDate date, int position, int reps, int load, boolean clean) throws Exception {
        mvc.perform(post("/api/sessions/" + date + "/exercises/" + position + "/sets").with(as(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reps":%d,"loadLb":%d,"clean":%b}""".formatted(reps, load, clean)))
                .andExpect(status().isCreated());
    }

    /* yesterday: 4 clean bench sets of 8 x 135 and 2 rows of 10 x 95, one not clean; today: nothing logged */
    private void twoSessions(UserAccount user) throws Exception {
        session(user, yesterday);
        for(int i = 0; i < 4; i++) logSet(user, yesterday, 1, 8, 135, true);
        logSet(user, yesterday, 2, 10, 95, true);
        logSet(user, yesterday, 2, 10, 95, false);
        session(user, today);
    }

    private String range() {
        return "?from=" + yesterday + "&to=" + today;
    }

    @Test
    void trainingReportHasARowPerSessionNewestFirst() throws Exception {
        twoSessions(owner);
        mvc.perform(get("/api/reports/training" + range()).with(as(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Training Report"))
                .andExpect(jsonPath("$.generatedAt").isNotEmpty())
                .andExpect(jsonPath("$.from").value(yesterday.toString()))
                .andExpect(jsonPath("$.rows", hasSize(2)))
                .andExpect(jsonPath("$.rows[0].date").value(today.toString()))
                .andExpect(jsonPath("$.rows[0].setsLogged").value(0))
                .andExpect(jsonPath("$.rows[0].cleanPercent").value(nullValue()))
                .andExpect(jsonPath("$.rows[1].date").value(yesterday.toString()))
                .andExpect(jsonPath("$.rows[1].workout").value("Upper A"))
                .andExpect(jsonPath("$.rows[1].tier").value("Full"))
                .andExpect(jsonPath("$.rows[1].location").value("Kaimana Downtown"))
                .andExpect(jsonPath("$.rows[1].exercisesDone").value(2))
                .andExpect(jsonPath("$.rows[1].exercisesPlanned").value(6))
                .andExpect(jsonPath("$.rows[1].setsLogged").value(6))
                .andExpect(jsonPath("$.rows[1].cleanPercent").value(83))
                .andExpect(jsonPath("$.rows[1].volumeLb").value(6220.0))
                .andExpect(jsonPath("$.sessions").value(2))
                .andExpect(jsonPath("$.setsLogged").value(6))
                .andExpect(jsonPath("$.volumeLb").value(6220.0));
    }

    @Test
    void balanceReportListsEveryMuscleAndPushPull() throws Exception {
        twoSessions(owner);
        mvc.perform(get("/api/reports/balance" + range()).with(as(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Training Balance Report"))
                .andExpect(jsonPath("$.generatedAt").isNotEmpty())
                .andExpect(jsonPath("$.totalSets").value(6))
                .andExpect(jsonPath("$.muscles", hasSize(15)))
                .andExpect(jsonPath("$.muscles[0].muscle").value("Chest"))
                .andExpect(jsonPath("$.muscles[0].sets").value(4))
                .andExpect(jsonPath("$.muscles[0].sharePercent").value(67))
                .andExpect(jsonPath("$.muscles[0].sessions").value(1))
                .andExpect(jsonPath("$.muscles[0].lastTrained").value(yesterday.toString()))
                .andExpect(jsonPath("$.muscles[1].muscle").value("Upper Back"))
                .andExpect(jsonPath("$.muscles[1].sharePercent").value(33))
                .andExpect(jsonPath("$.muscles[2].sets").value(0))
                .andExpect(jsonPath("$.muscles[2].lastTrained").value(nullValue()))
                .andExpect(jsonPath("$.directions[0].direction").value("Push"))
                .andExpect(jsonPath("$.directions[0].sets").value(4))
                .andExpect(jsonPath("$.directions[1].direction").value("Pull"))
                .andExpect(jsonPath("$.directions[1].sets").value(2))
                .andExpect(jsonPath("$.pushPullRatio").value(2.0));
    }

    @Test
    void anotherUsersSessionsAreNotCounted() throws Exception {
        twoSessions(other);
        mvc.perform(get("/api/reports/training" + range()).with(as(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows", empty()))
                .andExpect(jsonPath("$.setsLogged").value(0));
        mvc.perform(get("/api/reports/balance" + range()).with(as(owner)))
                .andExpect(jsonPath("$.totalSets").value(0))
                .andExpect(jsonPath("$.muscles", hasSize(15)))
                .andExpect(jsonPath("$.pushPullRatio").value(nullValue()));
    }

    @Test
    void sessionsOutsideTheRangeAreLeftOut() throws Exception {
        twoSessions(owner);
        mvc.perform(get("/api/reports/training?from=" + today + "&to=" + today).with(as(owner)))
                .andExpect(jsonPath("$.rows", hasSize(1)))
                .andExpect(jsonPath("$.rows[0].date").value(today.toString()));
    }

    @Test
    void noRangeMeansTheLast28Days() throws Exception {
        mvc.perform(get("/api/reports/training").with(as(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.to").value(today.toString()))
                .andExpect(jsonPath("$.from").value(today.minusDays(27).toString()));
    }

    @Test
    void backwardsRangeIsRejected() throws Exception {
        mvc.perform(get("/api/reports/balance?from=" + today + "&to=" + yesterday).with(as(owner)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("The start date must be on or before the end date"));
    }

    @Test
    void rangeOverAYearIsRejected() throws Exception {
        mvc.perform(get("/api/reports/training?from=" + today.minusDays(366) + "&to=" + today).with(as(owner)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("The range must be 366 days or fewer"));
    }

    @Test
    void badDateIsRejected() throws Exception {
        mvc.perform(get("/api/reports/training?from=yesterday").with(as(owner)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reportsNeedAToken() throws Exception {
        mvc.perform(get("/api/reports/training")).andExpect(status().isUnauthorized());
    }
}

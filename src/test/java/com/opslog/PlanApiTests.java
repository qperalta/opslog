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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PlanApiTests {

    @Autowired MockMvc mvc;
    @Autowired UserAccountRepository users;
    @Autowired LocationRepository locations;
    @Autowired ProgramTemplateRepository templates;

    /* always ahead of today, so every day gets planned */
    private final LocalDate monday = LocalDate.now(ZoneOffset.UTC).with(TemporalAdjusters.next(DayOfWeek.MONDAY));

    private UserAccount owner;
    private UserAccount other;
    private ProgramTemplate upperLower;
    private ProgramTemplate fullBody;

    @BeforeEach
    void setUp() {
        owner = users.save(new UserAccount("owner@example.com", "x".repeat(60), "Owner"));
        other = users.save(new UserAccount("other@example.com", "x".repeat(60), "Other"));
        upperLower = template("Upper/Lower 4-Day");
        fullBody = template("Full Body 3-Day");
    }

    private ProgramTemplate template(String name) {
        return templates.findAll().stream().filter(t -> t.getName().equals(name)).findFirst().orElseThrow();
    }

    private long workoutId(ProgramTemplate template, String name) {
        return template.getDays().stream().filter(d -> d.getName().equals(name)).findFirst().map(TemplateDay::getId).orElseThrow();
    }

    private RequestPostProcessor as(UserAccount user) {
        return jwt().jwt(token -> token.subject(user.getId().toString()));
    }

    private void choose(UserAccount user, ProgramTemplate template) throws Exception {
        mvc.perform(put("/api/program").with(as(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"templateId":%d}""".formatted(template.getId())))
                .andExpect(status().isOk());
    }

    private ResultActions week(UserAccount user) throws Exception {
        return mvc.perform(get("/api/plan").param("week", monday.toString()).with(as(user)));
    }

    private ResultActions setDay(UserAccount user, LocalDate date, String body) throws Exception {
        return mvc.perform(put("/api/plan/" + date).with(as(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void weekNeedsAProgram() throws Exception {
        week(owner)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No program has been chosen yet"));
    }

    @Test
    void weekIsPlannedFromTheTemplateInRotationOrder() throws Exception {
        choose(owner, upperLower);
        week(owner)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.start").value(monday.toString()))
                .andExpect(jsonPath("$.end").value(monday.plusDays(6).toString()))
                .andExpect(jsonPath("$.program.template.name").value("Upper/Lower 4-Day"))
                .andExpect(jsonPath("$.days", hasSize(7)))
                .andExpect(jsonPath("$.days[*].mode", everyItem(is("AUTO"))))
                .andExpect(jsonPath("$.days[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.days[0].workout.name").value("Upper A"))
                .andExpect(jsonPath("$.days[1].workout.name").value("Lower A"))
                .andExpect(jsonPath("$.days[2].workout").value(nullValue()))
                .andExpect(jsonPath("$.days[3].workout.name").value("Upper B"))
                .andExpect(jsonPath("$.days[4].workout.name").value("Lower B"))
                .andExpect(jsonPath("$.days[0].updatedBy").value("RULES"));
    }

    @Test
    void restDayShiftsTheRestOfTheWeek() throws Exception {
        choose(owner, upperLower);
        setDay(owner, monday, """
                {"mode":"REST"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("REST"))
                .andExpect(jsonPath("$.workout").value(nullValue()))
                .andExpect(jsonPath("$.updatedBy").value("USER"));
        week(owner)
                .andExpect(jsonPath("$.days[0].workout").value(nullValue()))
                .andExpect(jsonPath("$.days[1].workout.name").value("Upper A"))
                .andExpect(jsonPath("$.days[3].workout.name").value("Lower A"))
                .andExpect(jsonPath("$.days[4].workout.name").value("Upper B"));
    }

    @Test
    void chosenWorkoutIsKeptAndTheRotationContinuesAfterIt() throws Exception {
        choose(owner, upperLower);
        week(owner);
        setDay(owner, monday.plusDays(2), """
                {"mode":"TRAINING","workoutId":%d}""".formatted(workoutId(upperLower, "Lower B")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workout.name").value("Lower B"));
        week(owner)
                .andExpect(jsonPath("$.days[*].workout.name", contains("Upper A", "Lower A", "Lower B", "Upper A", "Lower A")));
    }

    @Test
    void lockedDayKeepsItsWorkoutWhenTheProgramChanges() throws Exception {
        choose(owner, upperLower);
        week(owner);
        setDay(owner, monday, """
                {"mode":"AUTO","locked":true}""")
                .andExpect(jsonPath("$.locked").value(true))
                .andExpect(jsonPath("$.workout.name").value("Upper A"));
        choose(owner, fullBody);
        week(owner)
                .andExpect(jsonPath("$.days[0].workout.name").value("Upper A"))
                .andExpect(jsonPath("$.days[0].locked").value(true))
                .andExpect(jsonPath("$.days[1].workout").value(nullValue()))
                .andExpect(jsonPath("$.days[2].workout.name").value("Full Body A"))
                .andExpect(jsonPath("$.days[4].workout.name").value("Full Body B"));
    }

    @Test
    void workoutFromAnotherProgramIsRejected() throws Exception {
        choose(owner, upperLower);
        long fullBodyA = workoutId(fullBody, "Full Body A");
        setDay(owner, monday, """
                {"mode":"TRAINING","workoutId":%d}""".formatted(fullBodyA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Workout " + fullBodyA + " is not part of your program"));
    }

    @Test
    void dayLocationMustBeVisibleToTheUser() throws Exception {
        long garage = locations.save(new Location("Garage", owner.getId())).getId();
        choose(owner, upperLower);
        choose(other, upperLower);
        setDay(owner, monday, """
                {"mode":"AUTO","locationId":%d}""".formatted(garage))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location.name").value("Garage"));
        setDay(other, monday, """
                {"mode":"AUTO","locationId":%d}""".formatted(garage))
                .andExpect(status().isNotFound());
    }

    @Test
    void plansArePerUser() throws Exception {
        choose(owner, upperLower);
        setDay(owner, monday, """
                {"mode":"REST"}""");
        choose(other, upperLower);
        week(other).andExpect(jsonPath("$.days[0].workout.name").value("Upper A"));
    }

    @Test
    void invalidRequestsReturn400() throws Exception {
        choose(owner, upperLower);
        setDay(owner, monday, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.mode").value("Mode is required"));
        setDay(owner, monday, """
                {"mode":"SOMETIMES"}""")
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/plan").param("week", monday.plusYears(2).toString()).with(as(owner)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("The week must be within a year of today"));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/plan")).andExpect(status().isUnauthorized());
    }
}

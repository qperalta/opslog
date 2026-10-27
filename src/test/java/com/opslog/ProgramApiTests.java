package com.opslog;

import com.opslog.auth.UserAccount;
import com.opslog.auth.UserAccountRepository;
import com.opslog.location.Location;
import com.opslog.location.LocationRepository;
import com.opslog.plan.ProgramTemplateRepository;
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
class ProgramApiTests {

    @Autowired MockMvc mvc;
    @Autowired UserAccountRepository users;
    @Autowired LocationRepository locations;
    @Autowired ProgramTemplateRepository templates;

    private UserAccount owner;
    private UserAccount other;
    private long upperLower;
    private long fullBody;

    @BeforeEach
    void setUp() {
        owner = users.save(new UserAccount("owner@example.com", "x".repeat(60), "Owner"));
        other = users.save(new UserAccount("other@example.com", "x".repeat(60), "Other"));
        upperLower = templateId("Upper/Lower 4-Day");
        fullBody = templateId("Full Body 3-Day");
    }

    private long templateId(String name) {
        return templates.findAll().stream().filter(t -> t.getName().equals(name)).findFirst().orElseThrow().getId();
    }

    private RequestPostProcessor as(UserAccount user) {
        return jwt().jwt(token -> token.subject(user.getId().toString()));
    }

    private ResultActions choose(UserAccount user, String body) throws Exception {
        return mvc.perform(put("/api/program").with(as(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void templatesComeFromTheDatabaseWithWorkoutsInRotationOrder() throws Exception {
        mvc.perform(get("/api/programs").with(as(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", contains("Full Body 3-Day", "Upper/Lower 4-Day")))
                .andExpect(jsonPath("$[1].trainingDays", contains("MONDAY", "TUESDAY", "THURSDAY", "FRIDAY")))
                .andExpect(jsonPath("$[1].workouts[*].name", contains("Upper A", "Lower A", "Upper B", "Lower B")))
                .andExpect(jsonPath("$[1].workouts[0].exercises", hasSize(6)))
                .andExpect(jsonPath("$[1].workouts[0].exercises[0].exercise.name").value("Barbell Bench Press"))
                .andExpect(jsonPath("$[1].workouts[0].exercises[0].mainLift").value(true))
                .andExpect(jsonPath("$[1].workouts[0].exercises[0].target").value("5-8 reps"));
    }

    @Test
    void noProgramReturns404() throws Exception {
        mvc.perform(get("/api/program").with(as(owner)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No program has been chosen yet"));
    }

    @Test
    void choosingAProgramSavesItWithTheDefaultLocation() throws Exception {
        long downtown = locations.findVisibleTo(owner.getId()).getFirst().getId();
        choose(owner, """
                {"templateId":%d,"locationId":%d}""".formatted(upperLower, downtown))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.template.name").value("Upper/Lower 4-Day"))
                .andExpect(jsonPath("$.location.id").value(downtown))
                .andExpect(jsonPath("$.startedOn").value(LocalDate.now(ZoneOffset.UTC).toString()));
        mvc.perform(get("/api/program").with(as(owner)))
                .andExpect(jsonPath("$.template.id").value(upperLower));
    }

    @Test
    void switchingProgramsReplacesTheOldOne() throws Exception {
        choose(owner, """
                {"templateId":%d}""".formatted(upperLower)).andExpect(status().isOk());
        choose(owner, """
                {"templateId":%d}""".formatted(fullBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.template.name").value("Full Body 3-Day"))
                .andExpect(jsonPath("$.location").value(nullValue()));
    }

    @Test
    void programsArePerUser() throws Exception {
        choose(owner, """
                {"templateId":%d}""".formatted(upperLower));
        mvc.perform(get("/api/program").with(as(other))).andExpect(status().isNotFound());
    }

    @Test
    void anotherUsersLocationCannotBeTheDefault() throws Exception {
        long garage = locations.save(new Location("Garage", owner.getId())).getId();
        choose(other, """
                {"templateId":%d,"locationId":%d}""".formatted(upperLower, garage))
                .andExpect(status().isNotFound());
    }

    @Test
    void unknownOrMissingTemplateIsRejected() throws Exception {
        choose(owner, """
                {"templateId":999999}""")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Program 999999 was not found"));
        choose(owner, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.templateId").value("Program is required"));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/programs")).andExpect(status().isUnauthorized());
    }
}

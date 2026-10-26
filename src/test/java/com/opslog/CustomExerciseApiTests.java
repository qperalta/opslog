package com.opslog;

import com.opslog.auth.UserAccount;
import com.opslog.auth.UserAccountRepository;
import com.opslog.catalog.EquipmentRepository;
import com.opslog.catalog.Exercise;
import com.opslog.catalog.ExerciseRepository;
import com.opslog.catalog.MovementPatternRepository;
import com.opslog.catalog.MuscleGroupRepository;
import com.opslog.catalog.StrengthExercise;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CustomExerciseApiTests {

    @Autowired MockMvc mvc;
    @Autowired UserAccountRepository users;
    @Autowired ExerciseRepository exercises;
    @Autowired MuscleGroupRepository muscleGroups;
    @Autowired MovementPatternRepository movementPatterns;
    @Autowired EquipmentRepository equipment;

    private UserAccount owner;
    private UserAccount other;
    private long squat;
    private long quads;
    private long barbell;
    private long rack;

    @BeforeEach
    void setUp() {
        owner = users.save(new UserAccount("owner@example.com", "x".repeat(60), "Owner"));
        other = users.save(new UserAccount("other@example.com", "x".repeat(60), "Other"));
        squat = movementPatterns.findAllByOrderByNameAsc().stream()
                .filter(p -> p.getName().equals("Squat")).findFirst().orElseThrow().getId();
        quads = muscleGroups.findAllByOrderByNameAsc().stream()
                .filter(m -> m.getName().equals("Quadriceps")).findFirst().orElseThrow().getId();
        barbell = equipmentId("Barbell");
        rack = equipmentId("Squat Rack");
    }

    private long equipmentId(String name) {
        return equipment.findAllByOrderByNameAsc().stream()
                .filter(e -> e.getName().equals(name)).findFirst().orElseThrow().getId();
    }

    private long catalogId(String name) {
        return exercises.findAll().stream()
                .filter(e -> e.getOwnerId() == null && e.getName().equals(name))
                .findFirst().orElseThrow().getId();
    }

    private long saveCustom(UserAccount user, String name) {
        Exercise exercise = exercises.findById(catalogId("Back Squat")).orElseThrow();
        return exercises.save(new StrengthExercise(name, exercise.getMovementPattern(),
                exercise.getPrimaryMuscle(), user.getId(), 8, 12)).getId();
    }

    private RequestPostProcessor as(UserAccount user) {
        return jwt().jwt(token -> token.subject(user.getId().toString()));
    }

    private String strengthBody(String name, int repMin, int repMax, long... equipmentIds) {
        StringBuilder ids = new StringBuilder();
        for(long id : equipmentIds) ids.append(ids.isEmpty() ? "" : ",").append(id);
        return """
                {"kind":"strength","name":"%s","movementPatternId":%d,"primaryMuscleId":%d,
                 "repMin":%d,"repMax":%d,"equipmentIds":[%s]}""".formatted(name, squat, quads, repMin, repMax, ids);
    }

    @Test
    void createReturns201WithLocationAndTrimmedName() throws Exception {
        mvc.perform(post("/api/catalog/exercises").with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(strengthBody("  Pin Squat  ", 3, 5, barbell, rack)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/api/catalog/exercises/\\d+")))
                .andExpect(jsonPath("$.name").value("Pin Squat"))
                .andExpect(jsonPath("$.kind").value("STRENGTH"))
                .andExpect(jsonPath("$.target").value("3-5 reps"))
                .andExpect(jsonPath("$.custom").value(true))
                .andExpect(jsonPath("$.equipment[*].name", contains("Barbell", "Squat Rack")));
    }

    @Test
    void createTimedAndCardioKinds() throws Exception {
        mvc.perform(post("/api/catalog/exercises").with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"kind":"TIMED","name":"Sissy Squat Hold","movementPatternId":%d,
                                 "primaryMuscleId":%d,"targetSeconds":45}""".formatted(squat, quads)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.target").value("45 s"))
                .andExpect(jsonPath("$.equipment", hasSize(0)));
        mvc.perform(post("/api/catalog/exercises").with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"kind":"CARDIO","name":"Stair Sprints","movementPatternId":%d,
                                 "primaryMuscleId":%d,"durationMinutes":10}""".formatted(squat, quads)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.target").value("10 min"));
    }

    @Test
    void invalidBodyReturns400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/catalog/exercises").with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"kind":"STRENGTH","name":"   ","repMin":0}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("Name is required"))
                .andExpect(jsonPath("$.errors.movementPatternId").value("Movement pattern is required"))
                .andExpect(jsonPath("$.errors.repMin").value("Minimum reps must be at least 1"));
    }

    @Test
    void domainRulesReturn400() throws Exception {
        mvc.perform(post("/api/catalog/exercises").with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(strengthBody("Pin Squat", 8, 5)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Maximum reps must be at least the minimum"));
        mvc.perform(post("/api/catalog/exercises").with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"kind":"TIMED","name":"Hold","movementPatternId":%d,"primaryMuscleId":%d}"""
                                .formatted(squat, quads)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Target time is required for this kind of exercise"));
        mvc.perform(post("/api/catalog/exercises").with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(strengthBody("Pin Squat", 3, 5, 999_999)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unknown equipment: [999999]"));
    }

    @Test
    void duplicateNameReturns409() throws Exception {
        saveCustom(owner, "Pin Squat");
        mvc.perform(post("/api/catalog/exercises").with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(strengthBody("PIN SQUAT", 3, 5)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("An exercise named \"PIN SQUAT\" already exists"));
        mvc.perform(post("/api/catalog/exercises").with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(strengthBody("back squat", 3, 5)))
                .andExpect(status().isConflict());
        // another user's names don't collide
        mvc.perform(post("/api/catalog/exercises").with(as(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(strengthBody("Pin Squat", 3, 5)))
                .andExpect(status().isCreated());
    }

    @Test
    void ownerCanUpdateAndKeepTheSameName() throws Exception {
        long id = saveCustom(owner, "Pin Squat");
        mvc.perform(put("/api/catalog/exercises/" + id).with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(strengthBody("Pin Squat", 2, 4, rack)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.target").value("2-4 reps"))
                .andExpect(jsonPath("$.equipment[*].name", contains("Squat Rack")));
        mvc.perform(get("/api/catalog/exercises/" + id).with(as(owner)))
                .andExpect(jsonPath("$.repMin").value(2))
                .andExpect(jsonPath("$.equipment", hasSize(1)));
    }

    @Test
    void updateCannotChangeKind() throws Exception {
        long id = saveCustom(owner, "Pin Squat");
        mvc.perform(put("/api/catalog/exercises/" + id).with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"kind":"TIMED","name":"Pin Squat","movementPatternId":%d,
                                 "primaryMuscleId":%d,"targetSeconds":30}""".formatted(squat, quads)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Exercise kind cannot be changed; create a new exercise instead"));
    }

    @Test
    void renameToAnExistingNameReturns409() throws Exception {
        saveCustom(owner, "Pin Squat");
        long id = saveCustom(owner, "Box Squat");
        mvc.perform(put("/api/catalog/exercises/" + id).with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(strengthBody("pin squat", 3, 5)))
                .andExpect(status().isConflict());
    }

    @Test
    void catalogExercisesAreReadOnly() throws Exception {
        long backSquat = catalogId("Back Squat");
        mvc.perform(get("/api/catalog/exercises/" + backSquat).with(as(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.custom").value(false));
        mvc.perform(put("/api/catalog/exercises/" + backSquat).with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(strengthBody("Back Squat", 1, 3)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Catalog exercises are read-only; create a custom exercise instead"));
        mvc.perform(delete("/api/catalog/exercises/" + backSquat).with(as(owner)))
                .andExpect(status().isForbidden());
    }

    @Test
    void anotherUsersExerciseLooksMissing() throws Exception {
        long id = saveCustom(owner, "Pin Squat");
        mvc.perform(get("/api/catalog/exercises/" + id).with(as(other)))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/catalog/exercises/" + id).with(as(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(strengthBody("Hijacked", 3, 5)))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/catalog/exercises/" + id).with(as(other)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/catalog/exercises/" + id).with(as(owner)))
                .andExpect(jsonPath("$.name").value("Pin Squat"));
    }

    @Test
    void ownerCanDelete() throws Exception {
        long id = saveCustom(owner, "Pin Squat");
        mvc.perform(delete("/api/catalog/exercises/" + id).with(as(owner)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/catalog/exercises/" + id).with(as(owner)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Exercise " + id + " was not found"));
    }

    @Test
    void writesRequireAuthentication() throws Exception {
        mvc.perform(post("/api/catalog/exercises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(strengthBody("Pin Squat", 3, 5)))
                .andExpect(status().isUnauthorized());
    }
}

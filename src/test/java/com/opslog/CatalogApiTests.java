package com.opslog;

import com.opslog.auth.UserAccount;
import com.opslog.auth.UserAccountRepository;
import com.opslog.catalog.ExerciseRepository;
import com.opslog.catalog.MovementPattern;
import com.opslog.catalog.MovementPatternRepository;
import com.opslog.catalog.MuscleGroup;
import com.opslog.catalog.MuscleGroupRepository;
import com.opslog.catalog.StrengthExercise;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CatalogApiTests {

    @Autowired MockMvc mvc;
    @Autowired UserAccountRepository users;
    @Autowired ExerciseRepository exercises;
    @Autowired MuscleGroupRepository muscleGroups;
    @Autowired MovementPatternRepository movementPatterns;

    private UserAccount owner;
    private UserAccount other;

    @BeforeEach
    void setUp() {
        owner = users.save(new UserAccount("owner@example.com", "x".repeat(60), "Owner"));
        other = users.save(new UserAccount("other@example.com", "x".repeat(60), "Other"));

        MovementPattern squat = movementPatterns.findAllByOrderByNameAsc().stream()
                .filter(p -> p.getName().equals("Squat")).findFirst().orElseThrow();
        MuscleGroup quads = muscleGroups.findAllByOrderByNameAsc().stream()
                .filter(m -> m.getName().equals("Quadriceps")).findFirst().orElseThrow();
        exercises.save(new StrengthExercise("Belt Squat", squat, quads, owner.getId(), 8, 12));
    }

    private RequestPostProcessor as(UserAccount user) {
        return jwt().jwt(token -> token.subject(user.getId().toString()));
    }

    @Test
    void lookupsRequireAuthentication() throws Exception {
        mvc.perform(get("/api/catalog/muscle-groups")).andExpect(status().isUnauthorized());
    }

    @Test
    void lookupsComeFromTheDatabaseSortedByName() throws Exception {
        mvc.perform(get("/api/catalog/muscle-groups").with(as(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(15)))
                .andExpect(jsonPath("$[0].name").value("Biceps"));
        mvc.perform(get("/api/catalog/movement-patterns").with(as(owner)))
                .andExpect(jsonPath("$", hasSize(13)));
        mvc.perform(get("/api/catalog/exercise-kinds").with(as(owner)))
                .andExpect(jsonPath("$", contains("CARDIO", "STRENGTH", "TIMED")));
    }

    @Test
    void equipmentIncludesKindAndMachineCap() throws Exception {
        mvc.perform(get("/api/catalog/equipment").with(as(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(27)))
                .andExpect(jsonPath("$[?(@.name == 'Cable Stack')].kind").value("MACHINE"))
                .andExpect(jsonPath("$[?(@.name == 'Cable Stack')].maxLoadLb").value(200.0));
    }

    @Test
    void searchByNameReturnsMultipleRowsWithDetails() throws Exception {
        mvc.perform(get("/api/catalog/exercises").param("q", "  SQUAT ").with(as(other)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.items[*].name", hasItems("Back Squat", "Bulgarian Split Squat")))
                .andExpect(jsonPath("$.items[?(@.name == 'Back Squat')].target").value("5-8 reps"))
                .andExpect(jsonPath("$.items[?(@.name == 'Back Squat')].equipment[*].name",
                        contains("Barbell", "Squat Rack")));
    }

    @Test
    void customExerciseIsVisibleOnlyToItsOwner() throws Exception {
        mvc.perform(get("/api/catalog/exercises").param("q", "belt").with(as(owner)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].custom").value(true));
        mvc.perform(get("/api/catalog/exercises").param("q", "belt").with(as(other)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void filtersCombine() throws Exception {
        Long dumbbell = exercises.findAll().stream()
                .flatMap(e -> e.getEquipment().stream())
                .filter(e -> e.getName().equals("Dumbbell")).findFirst().orElseThrow().getId();

        mvc.perform(get("/api/catalog/exercises")
                        .param("equipmentId", dumbbell.toString())
                        .param("kind", "timed")
                        .with(as(owner)))
                .andExpect(jsonPath("$.items[*].name", contains("Farmer's Carry")));
    }

    @Test
    void likeWildcardsAreLiteral() throws Exception {
        mvc.perform(get("/api/catalog/exercises").param("q", "%").with(as(owner)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void pagingReportsTotals() throws Exception {
        mvc.perform(get("/api/catalog/exercises").param("size", "10").param("page", "1").with(as(other)))
                .andExpect(jsonPath("$.items", hasSize(10)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalElements").value(81))
                .andExpect(jsonPath("$.totalPages").value(9));
    }

    @Test
    void invalidSearchReturns400() throws Exception {
        mvc.perform(get("/api/catalog/exercises").param("size", "500").with(as(owner)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.size").value("Page size must be 100 or fewer"));
        mvc.perform(get("/api/catalog/exercises").param("kind", "yoga").with(as(owner)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unknown exercise kind: yoga"));
    }
}

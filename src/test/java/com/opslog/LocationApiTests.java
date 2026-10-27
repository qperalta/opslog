package com.opslog;

import com.opslog.auth.UserAccount;
import com.opslog.auth.UserAccountRepository;
import com.opslog.catalog.EquipmentRepository;
import com.opslog.location.Location;
import com.opslog.location.LocationRepository;
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
class LocationApiTests {

    @Autowired MockMvc mvc;
    @Autowired UserAccountRepository users;
    @Autowired LocationRepository locations;
    @Autowired EquipmentRepository equipment;

    private UserAccount owner;
    private UserAccount other;
    private long dumbbell;
    private long band;

    @BeforeEach
    void setUp() {
        owner = users.save(new UserAccount("owner@example.com", "x".repeat(60), "Owner"));
        other = users.save(new UserAccount("other@example.com", "x".repeat(60), "Other"));
        dumbbell = equipmentId("Dumbbell");
        band = equipmentId("Resistance Band");
    }

    private long equipmentId(String name) {
        return equipment.findAllByOrderByNameAsc().stream()
                .filter(e -> e.getName().equals(name)).findFirst().orElseThrow().getId();
    }

    private long gymId(String name) {
        return locations.findVisibleTo(owner.getId()).stream()
                .filter(l -> l.getName().equals(name)).findFirst().orElseThrow().getId();
    }

    private long saveOwn(UserAccount user, String name) {
        return locations.save(new Location(name, user.getId())).getId();
    }

    private RequestPostProcessor as(UserAccount user) {
        return jwt().jwt(token -> token.subject(user.getId().toString()));
    }

    private String body(String name, long... equipmentIds) {
        StringBuilder ids = new StringBuilder();
        for(long id : equipmentIds) ids.append(ids.isEmpty() ? "" : ",").append(id);
        return """
                {"name":"%s","equipmentIds":[%s]}""".formatted(name, ids);
    }

    @Test
    void listShowsKaimanaGymsFirstThenOwnLocations() throws Exception {
        saveOwn(owner, "Hotel Gym");
        saveOwn(other, "Garage");
        mvc.perform(get("/api/locations").with(as(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name",
                        contains("Kaimana Downtown", "Kaimana Kailua", "Kaimana Kapolei", "Hotel Gym")))
                .andExpect(jsonPath("$[0].custom").value(false))
                .andExpect(jsonPath("$[3].custom").value(true));
    }

    @Test
    void seededGymsHaveTheirEquipment() throws Exception {
        mvc.perform(get("/api/locations/" + gymId("Kaimana Downtown")).with(as(owner)))
                .andExpect(jsonPath("$.equipment", hasSize(27)));
        mvc.perform(get("/api/locations/" + gymId("Kaimana Kailua")).with(as(owner)))
                .andExpect(jsonPath("$.equipment", hasSize(20)))
                .andExpect(jsonPath("$.equipment[*].name", not(hasItem("Leg Press Machine"))));
        mvc.perform(get("/api/locations/" + gymId("Kaimana Kapolei")).with(as(owner)))
                .andExpect(jsonPath("$.equipment", hasSize(9)))
                .andExpect(jsonPath("$.equipment[*].name", not(hasItem("Barbell"))));
    }

    @Test
    void createReturns201WithTrimmedNameAndEquipment() throws Exception {
        mvc.perform(post("/api/locations").with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("  Waikiki Hotel Gym ", dumbbell, band)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/api/locations/\\d+")))
                .andExpect(jsonPath("$.name").value("Waikiki Hotel Gym"))
                .andExpect(jsonPath("$.custom").value(true))
                .andExpect(jsonPath("$.equipment[*].name", contains("Dumbbell", "Resistance Band")));
    }

    @Test
    void invalidBodyReturns400() throws Exception {
        mvc.perform(post("/api/locations").with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("   ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("Name is required"));
        mvc.perform(post("/api/locations").with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Garage", 999_999)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unknown equipment: [999999]"));
    }

    @Test
    void duplicateNameReturns409() throws Exception {
        saveOwn(owner, "Garage");
        mvc.perform(post("/api/locations").with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("garage")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("A location named \"garage\" already exists"));
        mvc.perform(post("/api/locations").with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("KAIMANA KAILUA")))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/locations").with(as(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Garage")))
                .andExpect(status().isCreated());
    }

    @Test
    void ownerCanUpdateEquipmentAndKeepTheName() throws Exception {
        long id = saveOwn(owner, "Garage");
        mvc.perform(put("/api/locations/" + id).with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Garage", dumbbell)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipment[*].name", contains("Dumbbell")));
        mvc.perform(put("/api/locations/" + id).with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Home Garage", band)))
                .andExpect(jsonPath("$.name").value("Home Garage"))
                .andExpect(jsonPath("$.equipment[*].name", contains("Resistance Band")));
    }

    @Test
    void kaimanaGymsAreReadOnly() throws Exception {
        long downtown = gymId("Kaimana Downtown");
        mvc.perform(put("/api/locations/" + downtown).with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("My Downtown")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Kaimana gyms are read-only; create your own location instead"));
        mvc.perform(delete("/api/locations/" + downtown).with(as(owner)))
                .andExpect(status().isForbidden());
    }

    @Test
    void anotherUsersLocationLooksMissing() throws Exception {
        long id = saveOwn(owner, "Garage");
        mvc.perform(get("/api/locations/" + id).with(as(other)))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/locations/" + id).with(as(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Mine Now")))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/locations/" + id).with(as(other)))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownerCanDelete() throws Exception {
        long id = saveOwn(owner, "Garage");
        mvc.perform(delete("/api/locations/" + id).with(as(owner)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/locations/" + id).with(as(owner)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Location " + id + " was not found"));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/locations")).andExpect(status().isUnauthorized());
    }
}

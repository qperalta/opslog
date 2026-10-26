package com.opslog.catalog;

import com.opslog.auth.CurrentUser;
import com.opslog.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/* Lookup endpoints feed every catalog dropdown from the database */
@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService catalogService;
    private final CurrentUser currentUser;

    public CatalogController(CatalogService catalogService, CurrentUser currentUser) {
        this.catalogService = catalogService;
        this.currentUser = currentUser;
    }

    @GetMapping("/muscle-groups")
    public List<LookupResponse> muscleGroups() {
        return catalogService.muscleGroups();
    }

    @GetMapping("/movement-patterns")
    public List<LookupResponse> movementPatterns() {
        return catalogService.movementPatterns();
    }

    @GetMapping("/equipment")
    public List<EquipmentResponse> equipment() {
        return catalogService.equipment();
    }

    @GetMapping("/exercise-kinds")
    public List<String> exerciseKinds() {
        return catalogService.exerciseKinds();
    }

    /* query params bind onto the record, then @Valid runs its constraints */
    @GetMapping("/exercises")
    public PageResponse<ExerciseResponse> searchExercises(@Valid ExerciseSearch search) {
        return catalogService.searchExercises(currentUser.id(), search);
    }
}

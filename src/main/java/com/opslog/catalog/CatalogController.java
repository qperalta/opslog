package com.opslog.catalog;

import com.opslog.auth.CurrentUser;
import com.opslog.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/* Lookup endpoints feed every catalog dropdown from the database */
@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService catalogService;
    private final CustomExerciseService customExercises;
    private final CurrentUser currentUser;

    public CatalogController(CatalogService catalogService, CustomExerciseService customExercises,
                             CurrentUser currentUser) {
        this.catalogService = catalogService;
        this.customExercises = customExercises;
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

    @GetMapping("/exercises/{id}")
    public ExerciseResponse getExercise(@PathVariable long id) {
        return customExercises.get(currentUser.id(), id);
    }

    @PostMapping("/exercises")
    public ResponseEntity<ExerciseResponse> createExercise(@Valid @RequestBody ExerciseRequest request) {
        ExerciseResponse created = customExercises.create(currentUser.id(), request);
        return ResponseEntity.created(URI.create("/api/catalog/exercises/" + created.id())).body(created);
    }

    @PutMapping("/exercises/{id}")
    public ExerciseResponse updateExercise(@PathVariable long id, @Valid @RequestBody ExerciseRequest request) {
        return customExercises.update(currentUser.id(), id, request);
    }

    @DeleteMapping("/exercises/{id}")
    public ResponseEntity<Void> deleteExercise(@PathVariable long id) {
        customExercises.delete(currentUser.id(), id);
        return ResponseEntity.noContent().build();
    }
}

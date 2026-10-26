package com.opslog.catalog;

import com.opslog.common.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/* Relationships are LAZY and open-in-view is off, so entities are mapped to DTOs inside the transaction */
@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final MuscleGroupRepository muscleGroups;
    private final MovementPatternRepository movementPatterns;
    private final EquipmentRepository equipment;
    private final ExerciseRepository exercises;

    public CatalogService(MuscleGroupRepository muscleGroups, MovementPatternRepository movementPatterns,
                          EquipmentRepository equipment, ExerciseRepository exercises) {
        this.muscleGroups = muscleGroups;
        this.movementPatterns = movementPatterns;
        this.equipment = equipment;
        this.exercises = exercises;
    }

    public List<LookupResponse> muscleGroups() {
        return muscleGroups.findAllByOrderByNameAsc().stream().map(LookupResponse::from).toList();
    }

    public List<LookupResponse> movementPatterns() {
        return movementPatterns.findAllByOrderByNameAsc().stream().map(LookupResponse::from).toList();
    }

    public List<EquipmentResponse> equipment() {
        return equipment.findAllByOrderByNameAsc().stream().map(EquipmentResponse::from).toList();
    }

    public List<String> exerciseKinds() {
        return exercises.findDistinctKinds();
    }

    public PageResponse<ExerciseResponse> searchExercises(long userId, ExerciseSearch search) {
        Specification<Exercise> spec = Specification.allOf(
                ExerciseSpecifications.visibleTo(userId),
                ExerciseSpecifications.nameContains(search.q()),
                ExerciseSpecifications.hasPrimaryMuscle(search.muscleGroupId()),
                ExerciseSpecifications.hasMovementPattern(search.movementPatternId()),
                ExerciseSpecifications.usesEquipment(search.equipmentId()),
                ExerciseSpecifications.ofKind(search.kind()));
        Pageable pageable = PageRequest.of(search.page(), search.size(), Sort.by("name").and(Sort.by("id")));
        return PageResponse.from(exercises.findAll(spec, pageable).map(ExerciseResponse::from));
    }
}

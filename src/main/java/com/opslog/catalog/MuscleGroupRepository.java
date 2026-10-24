package com.opslog.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MuscleGroupRepository extends JpaRepository<MuscleGroup, Long> {

    List<MuscleGroup> findAllByOrderByNameAsc();
}

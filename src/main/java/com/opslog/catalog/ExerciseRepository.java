package com.opslog.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ExerciseRepository extends JpaRepository<Exercise, Long>, JpaSpecificationExecutor<Exercise> {

    List<Exercise> findAllByOrderByNameAsc();

    @Query(value = "SELECT DISTINCT kind FROM exercises ORDER BY kind", nativeQuery = true)
    List<String> findDistinctKinds();
}

package com.opslog.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ExerciseRepository extends JpaRepository<Exercise, Long>, JpaSpecificationExecutor<Exercise> {

    List<Exercise> findAllByOrderByNameAsc();

    @Query(value = "SELECT DISTINCT kind FROM exercises ORDER BY kind", nativeQuery = true)
    List<String> findDistinctKinds();

    /* a name is taken if a catalog row or one of the user's own rows already uses it (ignoring case) */
    @Query("""
            SELECT COUNT(e) > 0 FROM Exercise e
            WHERE LOWER(e.name) = LOWER(:name)
              AND (e.ownerId IS NULL OR e.ownerId = :ownerId)
              AND e.id <> :excludeId""")
    boolean isNameTaken(String name, long ownerId, long excludeId);
}

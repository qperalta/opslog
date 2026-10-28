package com.opslog.session;

import com.opslog.catalog.Exercise;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;

/* substitution candidates: catalog plus the user's own, equipment fetched in one query */
interface SessionCatalog extends Repository<Exercise, Long> {

    @Query("""
            SELECT DISTINCT e FROM Exercise e LEFT JOIN FETCH e.equipment
            WHERE e.ownerId IS NULL OR e.ownerId = :userId""")
    List<Exercise> findVisibleTo(long userId);
}

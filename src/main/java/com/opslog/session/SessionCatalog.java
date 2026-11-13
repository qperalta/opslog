package com.opslog.session;

import com.opslog.catalog.Exercise;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/* substitution candidates: catalog plus the user's own, equipment fetched in one query */
interface SessionCatalog extends Repository<Exercise, Long> {

    @Query("""
            SELECT DISTINCT e FROM Exercise e LEFT JOIN FETCH e.equipment
            WHERE e.ownerId IS NULL OR e.ownerId = :userId""")
    List<Exercise> findVisibleTo(long userId);

    @Query("SELECT e FROM Exercise e WHERE e.id = :id AND (e.ownerId IS NULL OR e.ownerId = :userId)")
    Optional<Exercise> findVisible(long id, long userId);

    /* names are unique per member ignoring case (catalog and own rows), so this finds at most one */
    @Query("SELECT e FROM Exercise e WHERE LOWER(e.name) = LOWER(:name) AND (e.ownerId IS NULL OR e.ownerId = :userId)")
    Optional<Exercise> findVisibleByName(String name, long userId);
}

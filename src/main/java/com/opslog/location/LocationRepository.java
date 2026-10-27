package com.opslog.location;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LocationRepository extends JpaRepository<Location, Long> {

    /* shared gyms first */
    @Query("""
            SELECT l FROM Location l
            WHERE l.ownerId IS NULL OR l.ownerId = :userId
            ORDER BY CASE WHEN l.ownerId IS NULL THEN 0 ELSE 1 END, l.name""")
    List<Location> findVisibleTo(long userId);

    @Query("""
            SELECT COUNT(l) > 0 FROM Location l
            WHERE LOWER(l.name) = LOWER(:name)
              AND (l.ownerId IS NULL OR l.ownerId = :ownerId)
              AND l.id <> :excludeId""")
    boolean isNameTaken(String name, long ownerId, long excludeId);
}

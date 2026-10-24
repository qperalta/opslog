package com.opslog.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovementPatternRepository extends JpaRepository<MovementPattern, Long> {

    List<MovementPattern> findAllByOrderByNameAsc();
}

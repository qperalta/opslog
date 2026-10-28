package com.opslog.logging;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.Repository;

/* the graph loads each row's session, exercise and muscle in the page query instead of one query per row */
interface LoggedSetRepository extends Repository<LoggedSet, Long>, JpaSpecificationExecutor<LoggedSet> {

    @Override
    @EntityGraph(attributePaths={"sessionExercise.session.workout", "sessionExercise.session.location",
            "sessionExercise.exercise.primaryMuscle"})
    Page<LoggedSet> findAll(Specification<LoggedSet> spec, Pageable pageable);
}

package com.opslog.plan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProgramTemplateRepository extends JpaRepository<ProgramTemplate, Long> {

    List<ProgramTemplate> findAllByOrderByNameAsc();
}

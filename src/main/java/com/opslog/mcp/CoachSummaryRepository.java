package com.opslog.mcp;

import org.springframework.data.jpa.repository.JpaRepository;

/* keyed by user id, so findById is already scoped to the member */
public interface CoachSummaryRepository extends JpaRepository<CoachSummary, Long> {
}

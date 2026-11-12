package com.opslog.session;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

/* one exercise as the agent sends it; AiSessionWriter checks it against the catalog and the location
 *  @Nullable marks the optional fields in the MCP tool's JSON schema */
public record ProposedExercise(Long exerciseId, Integer sets, @Nullable BigDecimal targetLoadLb, @Nullable String reason) {}

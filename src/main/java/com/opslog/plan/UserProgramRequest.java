package com.opslog.plan;

import jakarta.validation.constraints.NotNull;

/* locationId optional: the default gym for planned days */
public record UserProgramRequest(
        @NotNull(message="Program is required")
        Long templateId,

        Long locationId
) {}

package com.opslog.session;

import jakarta.validation.constraints.NotNull;

/* true skips the exercise, false re-activates it */
public record SkipRequest(
        @NotNull(message="Skipped is required")
        Boolean skipped
) {}

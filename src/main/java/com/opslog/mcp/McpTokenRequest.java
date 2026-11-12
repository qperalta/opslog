package com.opslog.mcp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record McpTokenRequest(
        @NotBlank(message="Name is required")
        @Size(max=60, message="Name must be 60 characters or fewer")
        String name,

        @NotNull(message="Scope is required")
        McpScope scope
) {
    public McpTokenRequest { name = name == null ? null : name.strip(); }
}

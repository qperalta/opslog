package com.opslog.location;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record LocationRequest(
        @NotBlank(message="Name is required")
        @Size(max=60, message="Name must be 60 characters or fewer")
        String name,

        @Size(max=100, message="A location can list at most 100 equipment items")
        Set<@NotNull Long> equipmentIds
) {
    public LocationRequest {
        name = name == null
                ? null
                : name.strip();
        equipmentIds = equipmentIds == null ? Set.of() : Set.copyOf(equipmentIds);
    }
}

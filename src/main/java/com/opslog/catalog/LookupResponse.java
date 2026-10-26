package com.opslog.catalog;

/* id + name pair that feeds dropdowns and nested references */
public record LookupResponse(Long id, String name) {

    static LookupResponse from(MuscleGroup muscle) {
        return new LookupResponse(muscle.getId(), muscle.getName());
    }

    static LookupResponse from(MovementPattern pattern) {
        return new LookupResponse(pattern.getId(), pattern.getName());
    }

    static LookupResponse from(Equipment item) {
        return new LookupResponse(item.getId(), item.getName());
    }
}

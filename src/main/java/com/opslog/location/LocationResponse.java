package com.opslog.location;

import com.opslog.catalog.LookupResponse;

import java.util.Comparator;
import java.util.List;

public record LocationResponse(Long id, String name, boolean custom, List<LookupResponse> equipment) {

    static LocationResponse from(Location location) {
        List<LookupResponse> equipment = location.getEquipment().stream()
                .map(item -> new LookupResponse(item.getId(), item.getName()))
                .sorted(Comparator.comparing(LookupResponse::name))
                .toList();
        return new LocationResponse(location.getId(), location.getName(), location.isCustom(), equipment);
    }
}

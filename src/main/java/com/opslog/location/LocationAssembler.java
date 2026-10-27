package com.opslog.location;

import com.opslog.catalog.Equipment;
import com.opslog.catalog.EquipmentRepository;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
class LocationAssembler {

    private final EquipmentRepository equipment;

    LocationAssembler(EquipmentRepository equipment) {
        this.equipment = equipment;
    }

    Location newLocation(long ownerId, LocationRequest request) {
        Location location = new Location(request.name(), ownerId);
        location.replaceEquipment(equipment(request.equipmentIds()));
        return location;
    }

    void applyTo(Location location, LocationRequest request) {
        location.setName(request.name());
        location.replaceEquipment(equipment(request.equipmentIds()));
    }

    private List<Equipment> equipment(Set<Long> ids) {
        List<Equipment> found = equipment.findAllById(ids);
        if(found.size() != ids.size()) {
            Set<Long> missing = new HashSet<>(ids);
            found.forEach(item -> missing.remove(item.getId()));
            throw new IllegalArgumentException("Unknown equipment: " + missing);
        }
        return found;
    }
}

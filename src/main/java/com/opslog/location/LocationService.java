package com.opslog.location;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class LocationService {

    private static final long NO_EXCLUSION = 0L;

    private final LocationRepository locations;
    private final LocationAssembler assembler;

    LocationService(LocationRepository locations, LocationAssembler assembler) {
        this.locations = locations;
        this.assembler = assembler;
    }

    @Transactional(readOnly = true)
    public List<LocationResponse> list(long userId) { return locations.findVisibleTo(userId).stream().map(LocationResponse::from).toList(); }

    @Transactional(readOnly = true)
    public LocationResponse get(long userId, long id) { return LocationResponse.from(findVisible(userId, id)); }

    public LocationResponse create(long userId, LocationRequest request) {
        requireUniqueName(userId, request.name(), NO_EXCLUSION);
        Location location = assembler.newLocation(userId, request);
        return LocationResponse.from(locations.saveAndFlush(location));
    }

    public LocationResponse update(long userId, long id, LocationRequest request) {
        Location location = findOwned(userId, id);
        requireUniqueName(userId, request.name(), id);
        assembler.applyTo(location, request);
        return LocationResponse.from(locations.saveAndFlush(location));
    }

    public void delete(long userId, long id) { locations.delete(findOwned(userId, id)); }

    /* used by check-in to confirm the location belongs to current user */
    @Transactional(readOnly = true)
    public Location findVisible(long userId, long id) {
        Location location = locations.findById(id).orElseThrow(() -> new LocationNotFoundException(id));
        if(!location.isVisibleTo(userId)) throw new LocationNotFoundException(id);
        return location;
    }

    private Location findOwned(long userId, long id) {
        Location location = findVisible(userId, id);
        if(!location.isCustom()) throw new LocationReadOnlyException();
        return location;
    }

    private void requireUniqueName(long userId, String name, long excludeId) { if(locations.isNameTaken(name, userId, excludeId)) throw new DuplicateLocationNameException(name); }
}

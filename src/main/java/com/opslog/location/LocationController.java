package com.opslog.location;

import com.opslog.auth.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationService locationService;
    private final CurrentUser currentUser;

    public LocationController(LocationService locationService, CurrentUser currentUser) {
        this.locationService = locationService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<LocationResponse> list() {
        return locationService.list(currentUser.id());
    }

    @GetMapping("/{id}")
    public LocationResponse get(@PathVariable long id) {
        return locationService.get(currentUser.id(), id);
    }

    @PostMapping
    public ResponseEntity<LocationResponse> create(@Valid @RequestBody LocationRequest request) {
        LocationResponse created = locationService.create(currentUser.id(), request);
        return ResponseEntity.created(URI.create("/api/locations/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public LocationResponse update(@PathVariable long id, @Valid @RequestBody LocationRequest request) {
        return locationService.update(currentUser.id(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        locationService.delete(currentUser.id(), id);
        return ResponseEntity.noContent().build();
    }
}

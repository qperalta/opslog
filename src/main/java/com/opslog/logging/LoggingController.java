package com.opslog.logging;

import com.opslog.auth.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/sessions/{date}/exercises/{position}/sets")
public class LoggingController {

    private final LoggingService loggingService;
    private final CurrentUser currentUser;

    public LoggingController(LoggingService loggingService, CurrentUser currentUser) {
        this.loggingService = loggingService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<LoggedSetResponse> log(@PathVariable @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date,
                                                 @PathVariable int position, @Valid @RequestBody LoggedSetRequest request) {
        LoggedSetResponse created = loggingService.log(currentUser.id(), date, position, request);
        return ResponseEntity.created(URI.create("/api/sessions/" + date + "/exercises/" + position + "/sets/" + created.setNumber()))
                .body(created);
    }

    @PutMapping("/{setNumber}")
    public LoggedSetResponse update(@PathVariable @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date,
                                    @PathVariable int position, @PathVariable int setNumber,
                                    @Valid @RequestBody LoggedSetRequest request) {
        return loggingService.update(currentUser.id(), date, position, setNumber, request);
    }

    @DeleteMapping("/{setNumber}")
    public ResponseEntity<Void> delete(@PathVariable @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date,
                                       @PathVariable int position, @PathVariable int setNumber) {
        loggingService.delete(currentUser.id(), date, position, setNumber);
        return ResponseEntity.noContent().build();
    }
}

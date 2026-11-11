package com.opslog.session;

import com.opslog.auth.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;
    private final CurrentUser currentUser;

    public SessionController(SessionService sessionService, CurrentUser currentUser) {
        this.sessionService = sessionService;
        this.currentUser = currentUser;
    }

    @GetMapping("/{date}")
    public SessionResponse get(@PathVariable @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date) {
        return sessionService.get(currentUser.id(), date);
    }

    @PostMapping("/{date}/rebuild")
    public SessionResponse rebuild(@PathVariable @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date) {
        return sessionService.rebuild(currentUser.id(), date);
    }

    @PutMapping("/{date}/exercises/{position}/skipped")
    public SessionExerciseResponse skip(@PathVariable @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date,
                                        @PathVariable int position, @Valid @RequestBody SkipRequest request) {
        return sessionService.setSkipped(currentUser.id(), date, position, request.skipped());
    }
}

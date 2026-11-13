package com.opslog.session;

import com.opslog.auth.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exercise-timings")
public class ExerciseTimingController {

    private final ExerciseTimingService timings;
    private final CurrentUser currentUser;

    public ExerciseTimingController(ExerciseTimingService timings, CurrentUser currentUser) {
        this.timings = timings;
        this.currentUser = currentUser;
    }

    @PutMapping("/{exerciseId}")
    public ExerciseTimingResponse set(@PathVariable long exerciseId, @Valid @RequestBody ExerciseTimingRequest request) {
        return timings.set(currentUser.id(), exerciseId, request.holdSeconds(), request.restSeconds());
    }
}

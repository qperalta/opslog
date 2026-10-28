package com.opslog.plan;

import com.opslog.auth.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ProgramController {

    private final ProgramService programService;
    private final PlanService planService;
    private final CurrentUser currentUser;

    public ProgramController(ProgramService programService, PlanService planService, CurrentUser currentUser) {
        this.programService = programService;
        this.planService = planService;
        this.currentUser = currentUser;
    }

    @GetMapping("/programs")
    public List<ProgramTemplateResponse> templates() {
        return programService.templates();
    }

    @GetMapping("/program")
    public UserProgramResponse get() {
        return programService.get(currentUser.id());
    }

    /* creates on first call, switches after */
    @PutMapping("/program")
    public UserProgramResponse choose(@Valid @RequestBody UserProgramRequest request) {
        UserProgramResponse program = programService.choose(currentUser.id(), request);
        /* switching templates re-plans the days ahead */
        planService.replanFrom(currentUser.id(), LocalDate.now(ZoneOffset.UTC));
        return program;
    }
}

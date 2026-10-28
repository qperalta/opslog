package com.opslog.plan;

import com.opslog.auth.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/api/plan")
public class PlanController {

    private final PlanService planService;
    private final PlanDayService planDayService;
    private final CurrentUser currentUser;

    public PlanController(PlanService planService, PlanDayService planDayService, CurrentUser currentUser) {
        this.planService = planService;
        this.planDayService = planDayService;
        this.currentUser = currentUser;
    }

    /* any date in the week; default this week */
    @GetMapping
    public WeekResponse week(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate week) {
        return planService.week(currentUser.id(), week != null ? week : LocalDate.now(ZoneOffset.UTC));
    }

    @PutMapping("/{date}")
    public PlanDayResponse setDay(@PathVariable @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date,
                                  @Valid @RequestBody PlanDayRequest request) {
        return planDayService.setDay(currentUser.id(), date, request, Actor.USER);
    }
}

package com.opslog.checkin;

import com.opslog.auth.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CheckInController {

    private final CheckInService checkInService;
    private final CurrentUser currentUser;

    public CheckInController(CheckInService checkInService, CurrentUser currentUser) {
        this.checkInService = checkInService;
        this.currentUser = currentUser;
    }

    @GetMapping("/tiers")
    public List<TierResponse> tiers() {
        return checkInService.tiers();
    }

    /* creates on first call, updates after */
    @PutMapping("/checkins/{date}")
    public ResponseEntity<CheckInResponse> save(@PathVariable @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date,
                                                @Valid @RequestBody CheckInRequest request) {
        CheckInService.Saved saved = checkInService.save(currentUser.id(), date, request);
        return ResponseEntity.status(saved.created() ? HttpStatus.CREATED : HttpStatus.OK).body(saved.checkIn());
    }

    @GetMapping("/checkins/{date}")
    public CheckInResponse get(@PathVariable @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date) {
        return checkInService.get(currentUser.id(), date);
    }

    /* default: last 14 days */
    @GetMapping("/checkins")
    public List<CheckInResponse> list(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
                                      @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now(ZoneOffset.UTC);
        LocalDate start = from != null ? from : end.minusDays(13);
        return checkInService.list(currentUser.id(), start, end);
    }
}

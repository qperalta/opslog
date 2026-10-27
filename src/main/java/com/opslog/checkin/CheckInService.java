package com.opslog.checkin;

import com.opslog.common.Require;
import com.opslog.location.Location;
import com.opslog.location.LocationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional
public class CheckInService {

    private static final long MAX_RANGE_DAYS = 366;

    private final CheckInRepository checkIns;
    private final TrainingTierRepository tiers;
    private final TierSelector tierSelector;
    private final LocationService locationService;

    CheckInService(CheckInRepository checkIns, TrainingTierRepository tiers, TierSelector tierSelector,
                   LocationService locationService) {
        this.checkIns = checkIns;
        this.tiers = tiers;
        this.tierSelector = tierSelector;
        this.locationService = locationService;
    }

    /* created -> 201, else 200 */
    public record Saved(CheckInResponse checkIn, boolean created) {}

    public Saved save(long userId, LocalDate date, CheckInRequest request) {
        /* +1 day for time zones ahead of UTC */
        Require.that(!date.isAfter(LocalDate.now(ZoneOffset.UTC).plusDays(1)), "Check-in date cannot be in the future");
        Location location = locationService.findVisible(userId, request.locationId());
        TrainingTier tier = tierSelector.select(request.sleepQuality(), request.soreness(), tiers.findAll());

        CheckIn existing = checkIns.findByUserIdAndDate(userId, date).orElse(null);
        CheckIn checkIn = existing != null ? existing : new CheckIn(userId, date);
        checkIn.record(request.sleepQuality(), request.soreness(), location, tier);
        return new Saved(CheckInResponse.from(checkIns.saveAndFlush(checkIn)), existing == null);
    }

    @Transactional(readOnly = true)
    public CheckInResponse get(long userId, LocalDate date) {
        return checkIns.findByUserIdAndDate(userId, date)
                .map(CheckInResponse::from)
                .orElseThrow(() -> new CheckInNotFoundException(date));
    }

    @Transactional(readOnly = true)
    public List<CheckInResponse> list(long userId, LocalDate from, LocalDate to) {
        Require.that(!from.isAfter(to), "The start date must be on or before the end date");
        Require.that(ChronoUnit.DAYS.between(from, to) < MAX_RANGE_DAYS, "The date range must be one year or less");
        return checkIns.findByUserIdAndDateBetweenOrderByDateDesc(userId, from, to).stream()
                .map(CheckInResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TierResponse> tiers() {
        return tiers.findAllByOrderByIntensityAsc().stream().map(TierResponse::from).toList();
    }
}

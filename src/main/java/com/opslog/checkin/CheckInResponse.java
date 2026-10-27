package com.opslog.checkin;

import com.opslog.catalog.LookupResponse;

import java.time.Instant;
import java.time.LocalDate;

public record CheckInResponse(Long id, LocalDate date, int sleepQuality, int soreness,
                              LookupResponse location, TierResponse tier, Instant updatedAt) {

    static CheckInResponse from(CheckIn checkIn) {
        /* null if the location was deleted */
        LookupResponse location = checkIn.getLocation() == null
                ? null
                : new LookupResponse(checkIn.getLocation().getId(), checkIn.getLocation().getName());
        return new CheckInResponse(checkIn.getId(), checkIn.getDate(), checkIn.getSleepQuality(),
                checkIn.getSoreness(), location, TierResponse.from(checkIn.getTier()), checkIn.getUpdatedAt());
    }
}

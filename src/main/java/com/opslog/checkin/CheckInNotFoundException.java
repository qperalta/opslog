package com.opslog.checkin;

import java.time.LocalDate;

public class CheckInNotFoundException extends RuntimeException {

    public CheckInNotFoundException(LocalDate date) {
        super("No check-in for " + date);
    }
}

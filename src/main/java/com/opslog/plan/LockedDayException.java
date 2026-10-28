package com.opslog.plan;

import java.time.LocalDate;

public class LockedDayException extends RuntimeException {

    public LockedDayException(LocalDate date) {
        super("The plan for " + date + " is locked");
    }
}

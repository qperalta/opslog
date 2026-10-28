package com.opslog.session;

import java.time.LocalDate;

/* rebuilding would delete the logged sets */
public class SessionHasLogsException extends RuntimeException {

    public SessionHasLogsException(LocalDate date) {
        super("The session for " + date + " already has logged sets and cannot be rebuilt");
    }
}

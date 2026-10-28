package com.opslog.logging;

import java.time.LocalDate;

/* also when there is no stored session for the date */
public class SessionExerciseNotFoundException extends RuntimeException {

    public SessionExerciseNotFoundException(LocalDate date, int position) {
        super("No exercise " + position + " in the session for " + date);
    }
}

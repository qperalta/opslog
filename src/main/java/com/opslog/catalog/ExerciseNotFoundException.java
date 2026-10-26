package com.opslog.catalog;

/* also thrown for another user's custom exercise, so its existence isn't leaked */
public class ExerciseNotFoundException extends RuntimeException {

    public ExerciseNotFoundException(long id) {
        super("Exercise " + id + " was not found");
    }
}

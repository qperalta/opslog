package com.opslog.catalog;

public class DuplicateExerciseNameException extends RuntimeException {

    public DuplicateExerciseNameException(String name) {
        super("An exercise named \"" + name + "\" already exists");
    }
}

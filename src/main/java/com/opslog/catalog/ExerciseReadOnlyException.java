package com.opslog.catalog;

public class ExerciseReadOnlyException extends RuntimeException {

    public ExerciseReadOnlyException() {
        super("Catalog exercises are read-only; create a custom exercise instead");
    }
}

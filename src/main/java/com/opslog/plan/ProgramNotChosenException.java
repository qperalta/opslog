package com.opslog.plan;

public class ProgramNotChosenException extends RuntimeException {

    public ProgramNotChosenException() {
        super("No program has been chosen yet");
    }
}

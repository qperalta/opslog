package com.opslog.logging;

public class LoggedSetNotFoundException extends RuntimeException {

    public LoggedSetNotFoundException(int setNumber) {
        super("Set " + setNumber + " was not found");
    }
}

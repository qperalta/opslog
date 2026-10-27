package com.opslog.location;

public class DuplicateLocationNameException extends RuntimeException {

    public DuplicateLocationNameException(String name) {
        super("A location named \"" + name + "\" already exists");
    }
}

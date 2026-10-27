package com.opslog.plan;

public class ProgramTemplateNotFoundException extends RuntimeException {

    public ProgramTemplateNotFoundException(long id) {
        super("Program " + id + " was not found");
    }
}

package com.opslog.plan;

import com.opslog.catalog.LookupResponse;

import java.time.LocalDate;

public record UserProgramResponse(LookupResponse template, LookupResponse location, LocalDate startedOn) {

    static UserProgramResponse from(UserProgram program) {
        ProgramTemplate template = program.getTemplate();
        /* null if the location was deleted */
        LookupResponse location = program.getLocation() == null
                ? null
                : new LookupResponse(program.getLocation().getId(), program.getLocation().getName());
        return new UserProgramResponse(new LookupResponse(template.getId(), template.getName()), location,
                program.getStartedOn());
    }
}

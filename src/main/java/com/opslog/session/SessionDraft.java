package com.opslog.session;

import com.opslog.common.Require;

import java.util.List;

/* generator output; notes feed "Why today looks like this" */
public record SessionDraft(List<DraftExercise> exercises, List<String> notes) {

    public SessionDraft {
        Require.that(exercises != null, "Exercises are required");
        exercises = List.copyOf(exercises);
        notes = notes == null ? List.of() : List.copyOf(notes);
    }

    public String rationale() { return String.join("\n", notes); }
}

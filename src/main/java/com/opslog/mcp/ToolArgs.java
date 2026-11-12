package com.opslog.mcp;

import com.opslog.catalog.LookupResponse;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

/* argument parsing shared by ReadTools and WriteTools; errors go back to the agent as tool errors */
final class ToolArgs {

    private ToolArgs() {}

    static LocalDate today() { return LocalDate.now(ZoneOffset.UTC); }

    static LocalDate dateOr(String text, LocalDate fallback) {
        if(text == null || text.isBlank()) return fallback;
        try {
            return LocalDate.parse(text.trim());
        } catch(DateTimeParseException ex) {
            throw new IllegalArgumentException("Dates must be YYYY-MM-DD, got " + text);
        }
    }

    /* the agent sends names, the services take ids; options come from the database */
    static Long idOf(String label, String name, List<LookupResponse> options) {
        if(name == null || name.isBlank()) return null;
        return options.stream()
                .filter(option -> option.name().equalsIgnoreCase(name.strip()))
                .findFirst()
                .map(LookupResponse::id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown " + label + " " + name.strip() + ". Use one of: "
                        + options.stream().map(LookupResponse::name).collect(Collectors.joining(", "))));
    }
}

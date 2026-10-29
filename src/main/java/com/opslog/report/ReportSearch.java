package com.opslog.report;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/* query-string range for GET /api/reports/*; both dates are optional */
public record ReportSearch(
        @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)
        LocalDate from,

        @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)
        LocalDate to
) {}

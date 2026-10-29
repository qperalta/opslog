package com.opslog.report;

/* movement_patterns.direction; patterns without one count in neither */
public enum Direction {
    PUSH("Push"),
    PULL("Pull");

    private final String label;

    Direction(String label) { this.label = label; }

    public String label() { return label; }
}

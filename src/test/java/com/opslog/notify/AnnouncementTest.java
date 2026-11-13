package com.opslog.notify;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AnnouncementTest {

    @Test void textIsTrimmed() {
        Announcement item = new Announcement("  New  ", "  Body  ", Instant.now());

        assertEquals("New", item.getTitle());
        assertEquals("Body", item.getBody());
    }

    @Test void titleAndBodyAreRequired() {
        assertThrows(IllegalArgumentException.class, () -> new Announcement(" ", "Body", Instant.now()));
        assertThrows(IllegalArgumentException.class, () -> new Announcement("New", "x".repeat(501), Instant.now()));
    }
}

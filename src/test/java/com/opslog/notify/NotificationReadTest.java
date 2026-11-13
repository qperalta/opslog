package com.opslog.notify;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class NotificationReadTest {

    private static final Instant NOON = Instant.parse("2026-11-13T12:00:00Z");

    @Test void laterReadMovesTheMarker() {
        NotificationRead read = new NotificationRead(7L, NOON);

        read.markRead(NOON.plusSeconds(60));

        assertEquals(NOON.plusSeconds(60), read.getReadAt());
    }

    @Test void olderReadNeverMovesItBack() {
        NotificationRead read = new NotificationRead(7L, NOON);

        read.markRead(NOON.minusSeconds(60));

        assertEquals(NOON, read.getReadAt());
    }

    @Test void userIsRequired() {
        assertThrows(IllegalArgumentException.class, () -> new NotificationRead(null, NOON));
    }
}

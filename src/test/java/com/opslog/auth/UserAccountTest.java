package com.opslog.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UserAccountTest {

    @Test
    void emailIsTrimmedAndLowercased() {
        UserAccount account = new UserAccount("   Que@mail.com ", "hash", "Que");

        assertEquals("que@mail.com", account.getEmail());
    }

    @Test
    void blankDisplayNameIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new UserAccount("que@mail.com", "hash", "   "));
    }

    @Test
    void displayNameLongerThanColumnIsRejected() {
        String fiftyOneLimit = "a".repeat(51);

        assertThrows(IllegalArgumentException.class,
                () -> new UserAccount("que@mail.com", "hash", fiftyOneLimit));
    }
}

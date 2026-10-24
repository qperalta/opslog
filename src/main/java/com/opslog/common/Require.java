package com.opslog.common;

/* Created a helper for conditional IllegalArgumentException throws.
*  note: the condition has to be reversed.
*/
public final class Require {

    private Require() {}

    public static void that(boolean condition, String message) {
        if(!condition) throw new IllegalArgumentException(message);
    }
}

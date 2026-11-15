package com.matriz.common;

/** Reads identifiers from URLs. */
public final class Identifiers {

    private Identifiers() {
    }

    /** Parses a path identifier; anything that is not a positive whole number is a {@code 400}. */
    public static long parse(String raw) {
        try {
            long id = Long.parseLong(raw);
            if (id >= 1) {
                return id;
            }
        } catch (NumberFormatException ignored) {
            // falls through to the error below
        }
        throw ApiException.badRequest("Identifiers must be positive whole numbers.");
    }
}

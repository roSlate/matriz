package com.matriz.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Collects the problems found in a request and reports them together as a {@code 400} with a
 * list of field errors. Text limits apply to the <em>trimmed</em> value, counted in characters,
 * and whitespace-only text counts as empty (NFR-04, assumption A-13).
 */
public final class FieldValidation {

    private final List<Map<String, String>> errors = new ArrayList<>();

    private FieldValidation() {
    }

    public static FieldValidation create() {
        return new FieldValidation();
    }

    /** The value with surrounding whitespace removed, or null for null. */
    public static String trim(String value) {
        return value == null ? null : value.strip();
    }

    /** The trimmed value, or null when nothing is left: empty text is stored as null. */
    public static String blankToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }

    /** Required text of {@code min} to {@code max} characters after trimming. */
    public FieldValidation requiredText(String field, String value, int min, int max) {
        String trimmed = trim(value);
        if (trimmed == null || trimmed.isEmpty()) {
            errors.add(error(field, "Must not be empty."));
        } else if (trimmed.codePointCount(0, trimmed.length()) < min
                || trimmed.codePointCount(0, trimmed.length()) > max) {
            errors.add(error(field, "Must be between " + min + " and " + max + " characters."));
        }
        return this;
    }

    /** Optional text of at most {@code max} characters after trimming; empty text is allowed. */
    public FieldValidation optionalText(String field, String value, int max) {
        String trimmed = trim(value);
        if (trimmed != null && trimmed.codePointCount(0, trimmed.length()) > max) {
            errors.add(error(field, "Must be at most " + max + " characters."));
        }
        return this;
    }

    public FieldValidation range(String field, int value, int min, int max) {
        if (value < min || value > max) {
            errors.add(error(field, "Must be between " + min + " and " + max + "."));
        }
        return this;
    }

    /** Throws the {@code 400} response if anything was wrong. */
    public void throwIfInvalid() {
        if (!errors.isEmpty()) {
            ApiException exception = ApiException.badRequest("Validation failed.");
            exception.getBody().setProperty("errors", List.copyOf(errors));
            throw exception;
        }
    }

    private static Map<String, String> error(String field, String message) {
        return Map.of("field", field, "message", message);
    }
}

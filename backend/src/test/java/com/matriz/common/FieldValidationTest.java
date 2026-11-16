package com.matriz.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Unit tests of the validation rules shared by every module (NFR-04, A-13): text limits count the
 * trimmed value in characters, and whitespace-only text counts as empty. No Spring context or
 * database is needed.
 */
class FieldValidationTest {

    // ---- the title rule of a task: 3 to 150 characters after trimming ----

    @Test
    void aTitleInsideTheLimitsIsValid() {
        assertThatCode(() -> FieldValidation.create().requiredText("title", "Write the brief", 3, 150).throwIfInvalid())
                .doesNotThrowAnyException();
    }

    @Test
    void theBoundaryValuesOfTheTitleAreValid() {
        assertThatCode(() -> FieldValidation.create().requiredText("title", "abc", 3, 150).throwIfInvalid())
                .doesNotThrowAnyException();
        assertThatCode(() -> FieldValidation.create().requiredText("title", "x".repeat(150), 3, 150).throwIfInvalid())
                .doesNotThrowAnyException();
    }

    @Test
    void justOutsideTheBoundariesIsInvalid() {
        assertThat(fieldErrors(FieldValidation.create().requiredText("title", "ab", 3, 150)))
                .containsExactly(Map.of("field", "title", "message", "Must be between 3 and 150 characters."));
        assertThat(fieldErrors(FieldValidation.create().requiredText("title", "x".repeat(151), 3, 150)))
                .containsExactly(Map.of("field", "title", "message", "Must be between 3 and 150 characters."));
    }

    @Test
    void emptyNullAndWhitespaceOnlyTitlesAreRejectedAsEmpty() {
        for (String empty : new String[] {null, "", "   ", "\t", "\n  \t "}) {
            assertThat(fieldErrors(FieldValidation.create().requiredText("title", empty, 3, 150)))
                    .as("for %s", empty == null ? "null" : "'" + empty.replace("\n", "\\n").replace("\t", "\\t") + "'")
                    .containsExactly(Map.of("field", "title", "message", "Must not be empty."));
        }
    }

    @Test
    void theLimitsApplyToTheTrimmedValue() {
        // 2 characters plus spaces is still too short; 3 characters plus spaces is valid.
        assertThat(fieldErrors(FieldValidation.create().requiredText("title", "  ab  ", 3, 150))).hasSize(1);
        assertThatCode(() -> FieldValidation.create().requiredText("title", "  abc  ", 3, 150).throwIfInvalid())
                .doesNotThrowAnyException();
        // 150 characters plus surrounding spaces is valid, because the spaces are not counted.
        assertThatCode(() -> FieldValidation.create().requiredText("title", " " + "x".repeat(150) + " ", 3, 150).throwIfInvalid())
                .doesNotThrowAnyException();
    }

    @Test
    void charactersAreCountedNotUtf16Units() {
        // An emoji is one character for the user but two Java chars: 150 of them must still be valid.
        String emoji = "\uD83D\uDE00";
        assertThatCode(() -> FieldValidation.create().requiredText("title", emoji.repeat(150), 3, 150).throwIfInvalid())
                .doesNotThrowAnyException();
        assertThat(fieldErrors(FieldValidation.create().requiredText("title", emoji.repeat(151), 3, 150))).hasSize(1);
        assertThat(fieldErrors(FieldValidation.create().requiredText("title", emoji.repeat(2), 3, 150))).hasSize(1);
    }

    // ---- optional text (descriptions) ----

    @Test
    void anOptionalTextMayBeMissingOrEmptyButNotTooLong() {
        for (String fine : new String[] {null, "", "   ", "x".repeat(2000), " " + "x".repeat(2000) + " "}) {
            assertThatCode(() -> FieldValidation.create().optionalText("description", fine, 2000).throwIfInvalid())
                    .doesNotThrowAnyException();
        }
        assertThat(fieldErrors(FieldValidation.create().optionalText("description", "x".repeat(2001), 2000)))
                .containsExactly(Map.of("field", "description", "message", "Must be at most 2000 characters."));
    }

    // ---- whole numbers in a range (page, size) ----

    @Test
    void aNumberOutsideItsRangeIsInvalid() {
        assertThatCode(() -> FieldValidation.create().range("size", 1, 1, 100).range("size", 100, 1, 100).throwIfInvalid())
                .doesNotThrowAnyException();
        assertThat(fieldErrors(FieldValidation.create().range("size", 0, 1, 100))).hasSize(1);
        assertThat(fieldErrors(FieldValidation.create().range("size", 101, 1, 100))).hasSize(1);
    }

    // ---- the answer ----

    @Test
    void severalProblemsAreReportedTogetherAsOne400() {
        FieldValidation validation = FieldValidation.create()
                .requiredText("title", "  ", 3, 150)
                .optionalText("description", "x".repeat(2001), 2000);

        assertThatThrownBy(validation::throwIfInvalid)
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getBody().getDetail()).isEqualTo("Validation failed.");
                    assertThat(exception.getBody().getProperties()).containsKey("errors");
                    assertThat(errorsOf(exception)).extracting(error -> error.get("field"))
                            .containsExactly("title", "description");
                });
    }

    @Test
    void textIsTrimmedAndEmptyTextBecomesNull() {
        assertThat(FieldValidation.trim("  hello \n")).isEqualTo("hello");
        assertThat(FieldValidation.trim(null)).isNull();
        assertThat(FieldValidation.blankToNull("   ")).isNull();
        assertThat(FieldValidation.blankToNull("  hello ")).isEqualTo("hello");
    }

    // ---- helpers ----

    private static List<Map<String, String>> fieldErrors(FieldValidation validation) {
        try {
            validation.throwIfInvalid();
            return List.of();
        } catch (ApiException exception) {
            return errorsOf(exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, String>> errorsOf(ApiException exception) {
        return (List<Map<String, String>>) exception.getBody().getProperties().get("errors");
    }
}

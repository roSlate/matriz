package com.matriz.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * A failure that the API reports to the client as a Problem Details response (RFC 9457).
 * The texts are fixed by the factory methods, so nothing internal can leak (CC-02).
 */
public class ApiException extends ErrorResponseException {

    public ApiException(HttpStatus status, String detail, String code) {
        super(status, ProblemDetail.forStatusAndDetail(status, detail), null);
        if (code != null) {
            getBody().setProperty("code", code);
        }
    }

    /** The resource does not exist, or the user has no access to its project: the two look the same. */
    public static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "Resource not found.", null);
    }

    /** The user has access to the project but their role is too low for the operation. */
    public static ApiException forbidden() {
        return new ApiException(HttpStatus.FORBIDDEN, "You do not have permission to do this.", "ROLE_INSUFFICIENT");
    }

    /** The request is valid but clashes with the current state (for example, a duplicate). */
    public static ApiException conflict(String detail, String code) {
        return new ApiException(HttpStatus.CONFLICT, detail, code);
    }

    /** The request is well formed but refers to something that does not exist (for example, an unknown email). */
    public static ApiException unprocessable(String detail, String code) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, detail, code);
    }

    public static ApiException badRequest(String detail) {
        return new ApiException(HttpStatus.BAD_REQUEST, detail, null);
    }
}

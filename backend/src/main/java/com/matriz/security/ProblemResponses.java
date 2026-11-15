package com.matriz.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.csrf.CsrfException;

/**
 * Writes the errors that happen in the security filters in the same Problem Details format as
 * the rest of the API (RFC 9457). The texts are fixed: nothing from the request or from an
 * exception is copied into the response, so no internal information can leak (CC-02).
 */
final class ProblemResponses {

    private ProblemResponses() {
    }

    /** No valid session: {@code 401}. */
    static void unauthorized(HttpServletRequest request, HttpServletResponse response,
                             AuthenticationException exception) throws IOException {
        write(response, 401, "Unauthorized", "Authentication is required.", null);
    }

    /** A missing or wrong CSRF token, or a role that is too low: {@code 403} with a code that tells them apart. */
    static void forbidden(HttpServletRequest request, HttpServletResponse response,
                          AccessDeniedException exception) throws IOException {
        if (exception instanceof CsrfException) {
            write(response, 403, "Forbidden", "Missing or invalid CSRF token.", "CSRF_TOKEN_INVALID");
        } else {
            write(response, 403, "Forbidden", "You do not have permission to do this.", "ROLE_INSUFFICIENT");
        }
    }

    private static void write(HttpServletResponse response, int status, String title, String detail,
                              String code) throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        String codeField = code == null ? "" : ",\"code\":\"" + code + "\"";
        response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"" + title
                + "\",\"status\":" + status + ",\"detail\":\"" + detail + "\"" + codeField + "}");
    }
}

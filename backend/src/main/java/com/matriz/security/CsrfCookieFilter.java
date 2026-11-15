package com.matriz.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Makes sure the {@code XSRF-TOKEN} cookie is sent on every response, including a {@code 401}.
 * Spring loads the token lazily, so without this the browser would have no token before its
 * first login request. The client calls {@code GET /api/v1/users/me} when it starts.
 */
final class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token != null) {
            token.getToken(); // reading the value forces the cookie to be written
        }
        filterChain.doFilter(request, response);
    }
}

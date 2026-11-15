package com.matriz.project;

import com.matriz.common.ApiException;
import com.matriz.identity.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

/**
 * The <em>access step</em> (docs/architecture.md, section 5). It runs before the request body
 * is read: it finds the project in the URL, looks up the user's membership, and answers
 * {@code 404} if there is none. The role it finds is passed on as a request attribute. Because
 * it comes first, a non-member cannot learn that a project exists by sending a malformed body.
 */
@Component
public class AccessStepInterceptor implements HandlerInterceptor {

    /** Request attribute holding the user's {@link Role} in the project of the URL. */
    public static final String ROLE_ATTRIBUTE = "com.matriz.project.role";

    private final ProjectAccess access;

    public AccessStepInterceptor(ProjectAccess access) {
        this.access = access;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Map<String, String> pathVariables =
                (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        String rawId = pathVariables == null ? null : pathVariables.get("projectId");
        if (rawId == null) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        request.setAttribute(ROLE_ATTRIBUTE, access.requireMembership(user.id(), parseId(rawId)));
        return true;
    }

    private static long parseId(String raw) {
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

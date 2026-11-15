package com.matriz.task;

import com.matriz.common.ApiException;
import com.matriz.common.Identifiers;
import com.matriz.identity.UserPrincipal;
import com.matriz.project.AccessStepInterceptor;
import com.matriz.project.ProjectAccess;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

/**
 * The access step for routes that name a task (docs/architecture.md, section 5). A task is as
 * private as its project: this finds the task's project, asks the access policy for the user's
 * role there, and answers {@code 404} if there is none, <em>before</em> the request body is read.
 * A task that does not exist and a task the user may not see look exactly the same.
 */
@Component
class TaskAccessStepInterceptor implements HandlerInterceptor {

    private final TaskRepository tasks;
    private final ProjectAccess access;

    TaskAccessStepInterceptor(TaskRepository tasks, ProjectAccess access) {
        this.tasks = tasks;
        this.access = access;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Map<String, String> pathVariables =
                (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        String rawId = pathVariables == null ? null : pathVariables.get("taskId");
        if (rawId == null) {
            return true;
        }

        Long projectId = tasks.findProjectIdById(Identifiers.parse(rawId)).orElseThrow(ApiException::notFound);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        request.setAttribute(AccessStepInterceptor.ROLE_ATTRIBUTE, access.requireMembership(user.id(), projectId));
        return true;
    }
}

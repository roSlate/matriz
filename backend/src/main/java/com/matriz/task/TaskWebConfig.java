package com.matriz.task;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Puts the task access step in front of every route that has a task identifier in its path. */
@Configuration
class TaskWebConfig implements WebMvcConfigurer {

    private final TaskAccessStepInterceptor accessStep;

    TaskWebConfig(TaskAccessStepInterceptor accessStep) {
        this.accessStep = accessStep;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(accessStep).addPathPatterns("/api/v1/tasks/{taskId}", "/api/v1/tasks/{taskId}/**");
    }
}

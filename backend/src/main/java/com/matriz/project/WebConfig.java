package com.matriz.project;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Puts the access step in front of every route that has a project identifier in its path. */
@Configuration
class WebConfig implements WebMvcConfigurer {

    private final AccessStepInterceptor accessStep;

    WebConfig(AccessStepInterceptor accessStep) {
        this.accessStep = accessStep;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(accessStep)
                .addPathPatterns("/api/v1/projects/{projectId}", "/api/v1/projects/{projectId}/**");
    }
}

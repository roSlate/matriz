package com.matriz.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Temporary security rules for the project skeleton: only the health endpoint is open and
 * everything else is refused. The real configuration (session login, CSRF, 401 handling)
 * replaces this class in increment 1 of docs/implementation-plan.md.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(requests -> requests
                .requestMatchers("/api/v1/actuator/health").permitAll()
                .anyRequest().denyAll());
        return http.build();
    }
}

package com.matriz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the Matriz API, a modular monolith organised by domain
 * (identity, project, task, comment). See docs/architecture.md.
 */
@SpringBootApplication
public class MatrizApplication {

    public static void main(String[] args) {
        SpringApplication.run(MatrizApplication.class, args);
    }
}

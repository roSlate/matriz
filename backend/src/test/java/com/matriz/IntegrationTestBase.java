package com.matriz;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

// One PostgreSQL container, started once and shared by every integration test class.
abstract class IntegrationTestBase {

    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    static {
        postgres.start();
    }
}

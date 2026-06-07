package com.smartinventory;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: verifies the full Spring context (security, JPA, seeding) starts
 * against the in-memory H2 database defined in application-test.yml.
 */
@SpringBootTest
@ActiveProfiles("test")
class SmartInventoryApplicationTests {

    @Test
    void contextLoads() {
        // Passes if the application context starts without error.
    }
}

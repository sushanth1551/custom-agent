package com.customagent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke-test: verifies that the Spring application context loads without errors.
 * Uses the default H2 datasource configured in application.properties.
 */
@SpringBootTest
class CustomAgentApplicationTest {

    @Test
    void contextLoads() {
        // If the context fails to start the test will throw before reaching this line.
    }
}

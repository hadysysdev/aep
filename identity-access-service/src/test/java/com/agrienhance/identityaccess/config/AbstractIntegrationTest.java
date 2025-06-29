package com.agrienhance.identityaccess.config;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.keycloak.admin.client.Keycloak;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES_CONTAINER;
    static final KeycloakContainer KEYCLOAK_CONTAINER;

    static {
        POSTGRES_CONTAINER = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("test_db_iam")
                .withUsername("user")
                .withPassword("password");

        KEYCLOAK_CONTAINER = new KeycloakContainer("keycloak/keycloak:25.0.2");

        POSTGRES_CONTAINER.start();
        KEYCLOAK_CONTAINER.start();
    }

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
    }

    @TestConfiguration
    public static class KeycloakTestConfig {
        @Bean
        @Primary
        public Keycloak keycloakAdminClient() {
            // This bean overrides the production one, connecting to the test container
            // instead of the properties in application.yml.
            return KEYCLOAK_CONTAINER.getKeycloakAdminClient();
        }
    }

}
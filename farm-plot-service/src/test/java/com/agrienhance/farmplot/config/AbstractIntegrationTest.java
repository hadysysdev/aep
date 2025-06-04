package com.agrienhance.farmplot.config;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

// This class can be inherited by actual test classes
// It does NOT need @Testcontainers itself if the @Container is in the subclass.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = AbstractIntegrationTest.DataSourceInitializer.class)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    // Shared container definition for subclasses to use with @Container
    public static final PostgreSQLContainer<?> POSTGIS_CONTAINER = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("test_db_farmplot")
            .withUsername("testuser")
            .withPassword("testpass");
    // .withInitScript("init_postgis.sql"); // If you need to ensure CREATE
    // EXTENSION postgis;

    // Static block to start the container once for all tests inheriting this
    // This is one way to have a singleton container for the entire test suite run
    static {
        POSTGIS_CONTAINER.start();
    }

    public static class DataSourceInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                    applicationContext,
                    "spring.datasource.url=" + POSTGIS_CONTAINER.getJdbcUrl(),
                    "spring.datasource.username=" + POSTGIS_CONTAINER.getUsername(),
                    "spring.datasource.password=" + POSTGIS_CONTAINER.getPassword(),
                    "spring.datasource.driver-class-name=org.postgresql.Driver",
                    "spring.jpa.hibernate.ddl-auto=none", // Flyway/Liquibase MUST handle schema. 'validate' is also an
                                                          // option.
                    "spring.flyway.enabled=true", // Ensure Flyway runs
                    "spring.flyway.locations=classpath:db/migration,classpath:db/testdata" // Include test data if
                                                                                           // needed
            // "spring.liquibase.enabled=true" // Or Liquibase
            );
        }
    }
}
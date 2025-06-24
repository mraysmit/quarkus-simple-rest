package dev.mars.testcontainers;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

/**
 * TestContainers resource for PostgreSQL database integration testing.
 * This demonstrates how to use TestContainers with Quarkus for integration testing.
 *
 * Note: This approach shows TestContainers setup but requires the application
 * to be built with PostgreSQL support. For this demo, we'll use it to show
 * the TestContainers integration pattern.
 */
public class PostgreSQLTestResource implements QuarkusTestResourceLifecycleManager {

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:15-alpine");

    private PostgreSQLContainer<?> postgres;

    @Override
    public Map<String, String> start() {
        postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE)
                .withDatabaseName("trading_test")
                .withUsername("test_user")
                .withPassword("test_password")
                .withReuse(true); // Reuse container across test runs for performance

        postgres.start();

        // For demonstration: These properties would configure Quarkus
        // if the application was built with PostgreSQL support
        return Map.of(
                "quarkus.datasource.username", postgres.getUsername(),
                "quarkus.datasource.password", postgres.getPassword(),
                "quarkus.datasource.jdbc.url", postgres.getJdbcUrl(),
                "quarkus.datasource.jdbc.max-size", "10",
                "quarkus.hibernate-orm.database.generation", "drop-and-create",
                "quarkus.hibernate-orm.log.sql", "false"
        );
    }

    @Override
    public void stop() {
        if (postgres != null && postgres.isRunning()) {
            postgres.stop();
        }
    }

    @Override
    public void inject(TestInjector testInjector) {
        // Make the container available for injection if needed
        testInjector.injectIntoFields(postgres,
            new TestInjector.AnnotatedAndMatchesType(InjectPostgreSQLContainer.class, PostgreSQLContainer.class));
    }
}

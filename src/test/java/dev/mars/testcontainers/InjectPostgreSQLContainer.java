package dev.mars.testcontainers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to inject PostgreSQL TestContainer instance into test fields.
 * This allows direct access to the container for advanced testing scenarios.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface InjectPostgreSQLContainer {
}

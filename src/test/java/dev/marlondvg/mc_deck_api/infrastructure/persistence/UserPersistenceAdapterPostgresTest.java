package dev.marlondvg.mc_deck_api.infrastructure.persistence;

import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Runs every {@link UserPersistenceAdapterTest} case against PostgreSQL.
 */
@Testcontainers(disabledWithoutDocker = true)
@Import({UserPersistenceAdapter.class, PostgresTestcontainersConfig.class})
class UserPersistenceAdapterPostgresTest extends UserPersistenceAdapterTest {
}

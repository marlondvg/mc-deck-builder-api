package dev.marlondvg.mc_deck_api.infrastructure.persistence;

import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Runs every {@link CollectionPersistenceAdapterTest} case against PostgreSQL.
 */
@Testcontainers(disabledWithoutDocker = true)
@Import({CollectionPersistenceAdapter.class, PostgresTestcontainersConfig.class})
class CollectionPersistenceAdapterPostgresTest extends CollectionPersistenceAdapterTest {
}

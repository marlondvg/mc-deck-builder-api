package dev.marlondvg.mc_deck_api.infrastructure.persistence;

import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Runs every {@link DeckPersistenceAdapterTest} case against PostgreSQL.
 */
@Testcontainers(disabledWithoutDocker = true)
@Import({DeckPersistenceAdapter.class, PostgresTestcontainersConfig.class})
class DeckPersistenceAdapterPostgresTest extends DeckPersistenceAdapterTest {
}

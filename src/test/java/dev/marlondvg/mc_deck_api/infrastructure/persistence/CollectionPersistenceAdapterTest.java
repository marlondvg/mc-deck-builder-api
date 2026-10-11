package dev.marlondvg.mc_deck_api.infrastructure.persistence;

import dev.marlondvg.mc_deck_api.domain.OwnedPacks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CollectionPersistenceAdapter.class)
class CollectionPersistenceAdapterTest {

	@Autowired
	private CollectionPersistenceAdapter adapter;

	@Autowired
	private TestEntityManager entityManager;

	private UUID ownerId;
	private UUID otherOwnerId;

	@BeforeEach
	void setUp() {
		ownerId = insertUser("ann");
		otherOwnerId = insertUser("bob");
	}

	private UUID insertUser(String username) {
		UUID id = UUID.randomUUID();
		entityManager.persist(new UserJpaEntity(id, username, "hash", Instant.parse("2026-01-01T10:00:00Z")));
		return id;
	}

	@Test
	void shouldBeEmptyForUserWithoutCollection() {
		assertThat(adapter.findByOwnerId(ownerId)).isEqualTo(OwnedPacks.empty());
	}

	@Test
	void shouldReplaceTheWholeCollection() {
		adapter.replace(ownerId, OwnedPacks.of(List.of("core", "cap")));
		adapter.replace(ownerId, OwnedPacks.of(List.of("core", "mut_gen")));
		entityManager.clear();

		assertThat(adapter.findByOwnerId(ownerId).codes()).containsExactly("core", "mut_gen");
	}

	@Test
	void shouldKeepCollectionsOfDifferentUsersApart() {
		adapter.replace(ownerId, OwnedPacks.of(List.of("core")));
		adapter.replace(otherOwnerId, OwnedPacks.of(List.of("cap")));
		adapter.replace(ownerId, OwnedPacks.empty());
		entityManager.clear();

		assertThat(adapter.findByOwnerId(ownerId).codes()).isEmpty();
		assertThat(adapter.findByOwnerId(otherOwnerId).codes()).containsExactly("cap");
	}
}

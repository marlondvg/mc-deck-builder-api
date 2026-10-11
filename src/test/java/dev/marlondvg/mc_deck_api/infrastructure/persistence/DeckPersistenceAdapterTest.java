package dev.marlondvg.mc_deck_api.infrastructure.persistence;

import dev.marlondvg.mc_deck_api.domain.Deck;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// Uses the H2 URL from application.yaml (PostgreSQL mode) instead of a plain embedded database.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(DeckPersistenceAdapter.class)
class DeckPersistenceAdapterTest {

	private static final Instant CREATED = Instant.parse("2026-01-01T10:00:00Z");

	@Autowired
	private DeckPersistenceAdapter adapter;

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
		entityManager.persist(new UserJpaEntity(id, username, "hash", CREATED));
		return id;
	}

	private Deck deck(UUID owner, String name, Instant updatedAt) {
		return Deck.restore(UUID.randomUUID(), owner, name, "04031a", List.of("protection", "aggression"),
				Map.of("01002", 1, "01060", 3), CREATED, updatedAt);
	}

	@Test
	void shouldSaveAndReadBackAllFields() {
		Deck deck = adapter.save(deck(ownerId, "Spider-Woman", CREATED));
		entityManager.flush();
		entityManager.clear();

		Deck found = adapter.findByIdAndOwnerId(deck.getId(), ownerId).orElseThrow();

		assertThat(found.getName()).isEqualTo("Spider-Woman");
		assertThat(found.getHeroCode()).isEqualTo("04031a");
		assertThat(found.getAspects()).containsExactly("protection", "aggression");
		assertThat(found.getSlots()).containsExactly(Map.entry("01002", 1), Map.entry("01060", 3));
		assertThat(found.getCreatedAt()).isEqualTo(CREATED);
	}

	@Test
	void shouldReplaceAspectsAndSlotsOnUpdate() {
		Deck deck = adapter.save(deck(ownerId, "Deck", CREATED));
		entityManager.flush();
		entityManager.clear();

		Deck loaded = adapter.findByIdAndOwnerId(deck.getId(), ownerId).orElseThrow();
		loaded.update("Deck", "04031a", List.of("justice"), Map.of("01061", 2), CREATED.plusSeconds(60));
		adapter.save(loaded);
		entityManager.flush();
		entityManager.clear();

		Deck found = adapter.findByIdAndOwnerId(deck.getId(), ownerId).orElseThrow();
		assertThat(found.getAspects()).containsExactly("justice");
		assertThat(found.getSlots()).containsExactly(Map.entry("01061", 2));
		assertThat(found.getUpdatedAt()).isEqualTo(CREATED.plusSeconds(60));
	}

	@Test
	void shouldOnlyFindDecksOfTheOwner() {
		Deck deck = adapter.save(deck(ownerId, "Mine", CREATED));
		adapter.save(deck(otherOwnerId, "Not mine", CREATED));
		entityManager.flush();
		entityManager.clear();

		assertThat(adapter.findByIdAndOwnerId(deck.getId(), otherOwnerId)).isEmpty();
		assertThat(adapter.findAllByOwnerId(ownerId)).extracting(Deck::getName).containsExactly("Mine");
	}

	@Test
	void shouldListMostRecentlyUpdatedFirst() {
		adapter.save(deck(ownerId, "Old", CREATED));
		adapter.save(deck(ownerId, "New", CREATED.plusSeconds(3600)));
		adapter.save(deck(ownerId, "Middle", CREATED.plusSeconds(60)));
		entityManager.flush();
		entityManager.clear();

		assertThat(adapter.findAllByOwnerId(ownerId)).extracting(Deck::getName)
				.containsExactly("New", "Middle", "Old");
	}

	@Test
	void shouldDeleteDeckWithItsAspectsAndSlots() {
		Deck deck = adapter.save(deck(ownerId, "Deck", CREATED));
		entityManager.flush();

		adapter.delete(deck);
		entityManager.flush();
		entityManager.clear();

		assertThat(adapter.findByIdAndOwnerId(deck.getId(), ownerId)).isEmpty();
		Number slots = (Number) entityManager.getEntityManager()
				.createNativeQuery("SELECT COUNT(*) FROM deck_slots").getSingleResult();
		assertThat(slots.intValue()).isZero();
	}
}

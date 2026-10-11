package dev.marlondvg.mc_deck_api.domain;

import dev.marlondvg.mc_deck_api.domain.exception.InvalidDeckException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeckTest {

	private static final UUID OWNER_ID = UUID.randomUUID();
	private static final Instant CREATED = Instant.parse("2026-01-01T10:00:00Z");
	private static final Instant LATER = Instant.parse("2026-01-02T10:00:00Z");
	private static final Map<String, Integer> SLOTS = Map.of("01002", 1, "01060", 3);

	private static Deck deck(String name, String heroCode, List<String> aspects, Map<String, Integer> slots) {
		return Deck.create(OWNER_ID, name, heroCode, aspects, slots, CREATED);
	}

	@Test
	void shouldCreateDeckWithTrimmedNameAndSameCreatedAndUpdatedDates() {
		Deck deck = deck("  Spider-Man – Justice ", "01001a", List.of("justice"), SLOTS);

		assertThat(deck.getId()).isNotNull();
		assertThat(deck.getOwnerId()).isEqualTo(OWNER_ID);
		assertThat(deck.getName()).isEqualTo("Spider-Man – Justice");
		assertThat(deck.getHeroCode()).isEqualTo("01001a");
		assertThat(deck.getAspects()).containsExactly("justice");
		assertThat(deck.getSlots()).isEqualTo(SLOTS);
		assertThat(deck.getCreatedAt()).isEqualTo(CREATED);
		assertThat(deck.getUpdatedAt()).isEqualTo(CREATED);
	}

	@Test
	void shouldKeepAspectOrderForMultiAspectHeroes() {
		Deck deck = deck("Spider-Woman", "04031a", List.of("protection", "aggression"), SLOTS);

		assertThat(deck.getAspects()).containsExactly("protection", "aggression");
	}

	@Test
	void shouldAcceptEmptySlotsAndDecksThatBreakGameRules() {
		// Deck size and copy limits are frontend rules; half-built decks must save.
		assertThat(deck("New deck", "01001a", List.of("justice"), Map.of()).getSlots()).isEmpty();
	}

	@Test
	void shouldUpdateContentsAndUpdatedDateButNotCreatedDate() {
		Deck deck = deck("Old", "01001a", List.of("justice"), SLOTS);

		deck.update("New", "01001a", List.of("aggression"), Map.of("01061", 2), LATER);

		assertThat(deck.getName()).isEqualTo("New");
		assertThat(deck.getAspects()).containsExactly("aggression");
		assertThat(deck.getSlots()).containsExactly(Map.entry("01061", 2));
		assertThat(deck.getCreatedAt()).isEqualTo(CREATED);
		assertThat(deck.getUpdatedAt()).isEqualTo(LATER);
	}

	@Test
	void shouldLeaveDeckUnchangedWhenUpdateIsInvalid() {
		Deck deck = deck("Old", "01001a", List.of("justice"), SLOTS);

		assertThatThrownBy(() -> deck.update("New", "01001a", List.of("pink"), SLOTS, LATER))
				.isInstanceOf(InvalidDeckException.class);
		assertThat(deck.getName()).isEqualTo("Old");
		assertThat(deck.getUpdatedAt()).isEqualTo(CREATED);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"   "})
	void shouldRejectMissingName(String name) {
		assertThatThrownBy(() -> deck(name, "01001a", List.of("justice"), SLOTS))
				.isInstanceOf(InvalidDeckException.class);
	}

	@Test
	void shouldRejectTooLongName() {
		assertThatThrownBy(() -> deck("x".repeat(Deck.MAX_NAME_LENGTH + 1), "01001a", List.of("justice"), SLOTS))
				.isInstanceOf(InvalidDeckException.class);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"01001A", "01 001a", "../etc", "123456789012345678901"})
	void shouldRejectInvalidHeroCode(String heroCode) {
		assertThatThrownBy(() -> deck("Deck", heroCode, List.of("justice"), SLOTS))
				.isInstanceOf(InvalidDeckException.class);
	}

	@Test
	void shouldRejectMissingUnknownOrRepeatedAspects() {
		assertThatThrownBy(() -> deck("Deck", "01001a", List.of(), SLOTS)).isInstanceOf(InvalidDeckException.class);
		assertThatThrownBy(() -> deck("Deck", "01001a", List.of("basic"), SLOTS))
				.isInstanceOf(InvalidDeckException.class);
		assertThatThrownBy(() -> deck("Deck", "01001a", List.of("justice", "justice"), SLOTS))
				.isInstanceOf(InvalidDeckException.class);
	}

	@Test
	void shouldAcceptAllFourAspectsButNotMore() {
		List<String> four = List.of("aggression", "justice", "leadership", "protection");
		assertThat(deck("Adam", "21031a", four, SLOTS).getAspects()).hasSize(4);
		List<String> five = List.of("aggression", "justice", "leadership", "protection", "pool");
		assertThatThrownBy(() -> deck("Adam", "21031a", five, SLOTS)).isInstanceOf(InvalidDeckException.class);
	}

	@Test
	void shouldRejectInvalidSlots() {
		assertThatThrownBy(() -> deck("Deck", "01001a", List.of("justice"), null))
				.isInstanceOf(InvalidDeckException.class);
		assertThatThrownBy(() -> deck("Deck", "01001a", List.of("justice"), Map.of("01002", 0)))
				.isInstanceOf(InvalidDeckException.class);
		assertThatThrownBy(() -> deck("Deck", "01001a", List.of("justice"), Map.of("01002", Deck.MAX_COPIES + 1)))
				.isInstanceOf(InvalidDeckException.class);
		assertThatThrownBy(() -> deck("Deck", "01001a", List.of("justice"), Map.of("bad code", 1)))
				.isInstanceOf(InvalidDeckException.class);
	}

	@Test
	void shouldRejectTooManyCardsInTotal() {
		Map<String, Integer> slots = new HashMap<>();
		for (int i = 0; i < 30; i++) {
			slots.put("card" + i, 7);
		}
		assertThatThrownBy(() -> deck("Deck", "01001a", List.of("justice"), slots))
				.isInstanceOf(InvalidDeckException.class)
				.hasMessageContaining("in total");
	}

	@Test
	void shouldNotLetCallersChangeSlotsOrAspects() {
		Deck deck = deck("Deck", "01001a", List.of("justice"), SLOTS);

		assertThatThrownBy(() -> deck.getSlots().put("01003", 1)).isInstanceOf(UnsupportedOperationException.class);
		assertThatThrownBy(() -> deck.getAspects().add("pool")).isInstanceOf(UnsupportedOperationException.class);
	}
}

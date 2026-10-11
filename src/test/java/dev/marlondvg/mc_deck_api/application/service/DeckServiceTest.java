package dev.marlondvg.mc_deck_api.application.service;

import dev.marlondvg.mc_deck_api.application.port.in.DeckContents;
import dev.marlondvg.mc_deck_api.application.port.out.DeckRepository;
import dev.marlondvg.mc_deck_api.domain.Deck;
import dev.marlondvg.mc_deck_api.domain.exception.DeckNotFoundException;
import dev.marlondvg.mc_deck_api.domain.exception.InvalidDeckException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeckServiceTest {

	private static final Instant CREATED = Instant.parse("2026-01-01T10:00:00Z");
	private static final Instant NOW = Instant.parse("2026-04-01T12:00:00Z");
	private static final UUID OWNER_ID = UUID.randomUUID();
	private static final UUID OTHER_OWNER_ID = UUID.randomUUID();
	private static final DeckContents CONTENTS =
			new DeckContents("Spider-Man", "01001a", List.of("justice"), Map.of("01002", 1));

	@Mock
	private DeckRepository deckRepository;

	private DeckService deckService;

	@BeforeEach
	void setUp() {
		deckService = new DeckService(deckRepository, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	private static Deck existingDeck() {
		return Deck.restore(UUID.randomUUID(), OWNER_ID, "Old", "01001a", List.of("justice"), Map.of(),
				CREATED, CREATED);
	}

	@Test
	void shouldCreateDeckForOwnerAtCurrentTime() {
		when(deckRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		Deck deck = deckService.createDeck(OWNER_ID, CONTENTS);

		assertThat(deck.getOwnerId()).isEqualTo(OWNER_ID);
		assertThat(deck.getName()).isEqualTo("Spider-Man");
		assertThat(deck.getCreatedAt()).isEqualTo(NOW);
		assertThat(deck.getUpdatedAt()).isEqualTo(NOW);
	}

	@Test
	void shouldNotSaveInvalidDeck() {
		DeckContents invalid = new DeckContents("Deck", "01001a", List.of("pink"), Map.of());

		assertThatThrownBy(() -> deckService.createDeck(OWNER_ID, invalid)).isInstanceOf(InvalidDeckException.class);
		verifyNoInteractions(deckRepository);
	}

	@Test
	void shouldGetAndListOnlyOwnersDecks() {
		Deck deck = existingDeck();
		when(deckRepository.findByIdAndOwnerId(deck.getId(), OWNER_ID)).thenReturn(Optional.of(deck));
		when(deckRepository.findAllByOwnerId(OWNER_ID)).thenReturn(List.of(deck));

		assertThat(deckService.getDeck(OWNER_ID, deck.getId())).isEqualTo(deck);
		assertThat(deckService.listDecks(OWNER_ID)).containsExactly(deck);
	}

	@Test
	void shouldUpdateDeckAndSetUpdatedDate() {
		Deck deck = existingDeck();
		when(deckRepository.findByIdAndOwnerId(deck.getId(), OWNER_ID)).thenReturn(Optional.of(deck));
		when(deckRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		Deck updated = deckService.updateDeck(OWNER_ID, deck.getId(), CONTENTS);

		assertThat(updated.getName()).isEqualTo("Spider-Man");
		assertThat(updated.getSlots()).containsExactly(Map.entry("01002", 1));
		assertThat(updated.getCreatedAt()).isEqualTo(CREATED);
		assertThat(updated.getUpdatedAt()).isEqualTo(NOW);
	}

	@Test
	void shouldTreatAnotherOwnersDeckAsNotFound() {
		UUID deckId = UUID.randomUUID();
		when(deckRepository.findByIdAndOwnerId(deckId, OTHER_OWNER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> deckService.getDeck(OTHER_OWNER_ID, deckId))
				.isInstanceOf(DeckNotFoundException.class);
		assertThatThrownBy(() -> deckService.updateDeck(OTHER_OWNER_ID, deckId, CONTENTS))
				.isInstanceOf(DeckNotFoundException.class);
		assertThatThrownBy(() -> deckService.deleteDeck(OTHER_OWNER_ID, deckId))
				.isInstanceOf(DeckNotFoundException.class);
		verify(deckRepository, never()).save(any());
		verify(deckRepository, never()).delete(any());
	}

	@Test
	void shouldDeleteOwnedDeck() {
		Deck deck = existingDeck();
		when(deckRepository.findByIdAndOwnerId(deck.getId(), OWNER_ID)).thenReturn(Optional.of(deck));

		deckService.deleteDeck(OWNER_ID, deck.getId());

		verify(deckRepository).delete(deck);
	}
}

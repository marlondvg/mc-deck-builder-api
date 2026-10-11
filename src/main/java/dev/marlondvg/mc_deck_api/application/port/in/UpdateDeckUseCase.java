package dev.marlondvg.mc_deck_api.application.port.in;

import dev.marlondvg.mc_deck_api.domain.Deck;

import java.util.UUID;

public interface UpdateDeckUseCase {

	Deck updateDeck(UUID ownerId, UUID deckId, DeckContents contents);
}

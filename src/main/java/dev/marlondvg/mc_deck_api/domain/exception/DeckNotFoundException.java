package dev.marlondvg.mc_deck_api.domain.exception;

import java.util.UUID;

public class DeckNotFoundException extends DomainException {

	public DeckNotFoundException(UUID deckId) {
		super("Deck not found: " + deckId);
	}
}

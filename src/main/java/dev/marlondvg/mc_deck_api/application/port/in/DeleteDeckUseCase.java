package dev.marlondvg.mc_deck_api.application.port.in;

import java.util.UUID;

public interface DeleteDeckUseCase {

	void deleteDeck(UUID ownerId, UUID deckId);
}

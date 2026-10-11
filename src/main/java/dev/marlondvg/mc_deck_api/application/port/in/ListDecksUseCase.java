package dev.marlondvg.mc_deck_api.application.port.in;

import dev.marlondvg.mc_deck_api.domain.Deck;

import java.util.List;
import java.util.UUID;

public interface ListDecksUseCase {

	/**
	 * The owner's decks, most recently updated first.
	 */
	List<Deck> listDecks(UUID ownerId);
}

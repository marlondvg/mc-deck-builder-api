package dev.marlondvg.mc_deck_api.application.port.out;

import dev.marlondvg.mc_deck_api.domain.Deck;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeckRepository {

	Deck save(Deck deck);

	Optional<Deck> findByIdAndOwnerId(UUID id, UUID ownerId);

	/**
	 * Returns the owner's decks, most recently updated first.
	 */
	List<Deck> findAllByOwnerId(UUID ownerId);

	void delete(Deck deck);
}

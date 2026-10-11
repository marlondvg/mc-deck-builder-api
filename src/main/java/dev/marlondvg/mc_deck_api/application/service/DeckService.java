package dev.marlondvg.mc_deck_api.application.service;

import dev.marlondvg.mc_deck_api.application.port.in.CreateDeckUseCase;
import dev.marlondvg.mc_deck_api.application.port.in.DeckContents;
import dev.marlondvg.mc_deck_api.application.port.in.DeleteDeckUseCase;
import dev.marlondvg.mc_deck_api.application.port.in.GetDeckUseCase;
import dev.marlondvg.mc_deck_api.application.port.in.ListDecksUseCase;
import dev.marlondvg.mc_deck_api.application.port.in.UpdateDeckUseCase;
import dev.marlondvg.mc_deck_api.application.port.out.DeckRepository;
import dev.marlondvg.mc_deck_api.domain.Deck;
import dev.marlondvg.mc_deck_api.domain.exception.DeckNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DeckService implements CreateDeckUseCase, GetDeckUseCase, ListDecksUseCase, UpdateDeckUseCase,
		DeleteDeckUseCase {

	private final DeckRepository deckRepository;
	private final Clock clock;

	public DeckService(DeckRepository deckRepository, Clock clock) {
		this.deckRepository = deckRepository;
		this.clock = clock;
	}

	@Override
	public Deck createDeck(UUID ownerId, DeckContents contents) {
		Deck deck = Deck.create(ownerId, contents.name(), contents.heroCode(), contents.aspects(),
				contents.slots(), Instant.now(clock));
		return deckRepository.save(deck);
	}

	@Override
	@Transactional(readOnly = true)
	public Deck getDeck(UUID ownerId, UUID deckId) {
		return findOwnedDeck(ownerId, deckId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Deck> listDecks(UUID ownerId) {
		return deckRepository.findAllByOwnerId(ownerId);
	}

	@Override
	public Deck updateDeck(UUID ownerId, UUID deckId, DeckContents contents) {
		Deck deck = findOwnedDeck(ownerId, deckId);
		deck.update(contents.name(), contents.heroCode(), contents.aspects(), contents.slots(), Instant.now(clock));
		return deckRepository.save(deck);
	}

	@Override
	public void deleteDeck(UUID ownerId, UUID deckId) {
		deckRepository.delete(findOwnedDeck(ownerId, deckId));
	}

	private Deck findOwnedDeck(UUID ownerId, UUID deckId) {
		return deckRepository.findByIdAndOwnerId(deckId, ownerId)
				.orElseThrow(() -> new DeckNotFoundException(deckId));
	}
}

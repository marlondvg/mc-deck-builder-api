package dev.marlondvg.mc_deck_api.infrastructure.persistence;

import dev.marlondvg.mc_deck_api.application.port.out.DeckRepository;
import dev.marlondvg.mc_deck_api.domain.Deck;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class DeckPersistenceAdapter implements DeckRepository {

	private final SpringDataDeckRepository repository;

	DeckPersistenceAdapter(SpringDataDeckRepository repository) {
		this.repository = repository;
	}

	@Override
	public Deck save(Deck deck) {
		return DeckMapper.toDomain(repository.save(DeckMapper.toEntity(deck)));
	}

	@Override
	public Optional<Deck> findByIdAndOwnerId(UUID id, UUID ownerId) {
		return repository.findByIdAndOwnerId(id, ownerId).map(DeckMapper::toDomain);
	}

	@Override
	public List<Deck> findAllByOwnerId(UUID ownerId) {
		return repository.findAllByOwnerIdOrderByUpdatedAtDesc(ownerId).stream().map(DeckMapper::toDomain).toList();
	}

	@Override
	public void delete(Deck deck) {
		repository.deleteById(deck.getId());
	}
}

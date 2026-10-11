package dev.marlondvg.mc_deck_api.infrastructure.persistence;

import dev.marlondvg.mc_deck_api.application.port.out.CollectionRepository;
import dev.marlondvg.mc_deck_api.domain.OwnedPacks;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
class CollectionPersistenceAdapter implements CollectionRepository {

	private final SpringDataOwnedPackRepository repository;

	CollectionPersistenceAdapter(SpringDataOwnedPackRepository repository) {
		this.repository = repository;
	}

	@Override
	public OwnedPacks findByOwnerId(UUID ownerId) {
		return OwnedPacks.of(repository.findAllByUserId(ownerId).stream()
				.map(OwnedPackJpaEntity::getPackCode)
				.toList());
	}

	@Override
	public void replace(UUID ownerId, OwnedPacks packs) {
		repository.deleteAllByUserId(ownerId);
		repository.saveAll(packs.codes().stream()
				.map(code -> new OwnedPackJpaEntity(ownerId, code))
				.toList());
		repository.flush();
	}
}

package dev.marlondvg.mc_deck_api.application.service;

import dev.marlondvg.mc_deck_api.application.port.in.GetCollectionUseCase;
import dev.marlondvg.mc_deck_api.application.port.in.ReplaceCollectionUseCase;
import dev.marlondvg.mc_deck_api.application.port.out.CollectionRepository;
import dev.marlondvg.mc_deck_api.domain.OwnedPacks;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.UUID;

@Service
@Transactional
public class CollectionService implements GetCollectionUseCase, ReplaceCollectionUseCase {

	private final CollectionRepository collectionRepository;

	public CollectionService(CollectionRepository collectionRepository) {
		this.collectionRepository = collectionRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public OwnedPacks getCollection(UUID ownerId) {
		return collectionRepository.findByOwnerId(ownerId);
	}

	@Override
	public OwnedPacks replaceCollection(UUID ownerId, Collection<String> packCodes) {
		// Validated before touching the database, so bad input changes nothing.
		OwnedPacks packs = OwnedPacks.of(packCodes);
		collectionRepository.replace(ownerId, packs);
		return packs;
	}
}

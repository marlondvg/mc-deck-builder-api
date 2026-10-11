package dev.marlondvg.mc_deck_api.application.port.in;

import dev.marlondvg.mc_deck_api.domain.OwnedPacks;

import java.util.UUID;

public interface GetCollectionUseCase {

	OwnedPacks getCollection(UUID ownerId);
}

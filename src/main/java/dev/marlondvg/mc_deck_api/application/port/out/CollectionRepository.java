package dev.marlondvg.mc_deck_api.application.port.out;

import dev.marlondvg.mc_deck_api.domain.OwnedPacks;

import java.util.UUID;

public interface CollectionRepository {

	/**
	 * The owner's packs; empty if nothing was saved yet.
	 */
	OwnedPacks findByOwnerId(UUID ownerId);

	/**
	 * Replaces every pack of the owner with the given ones.
	 */
	void replace(UUID ownerId, OwnedPacks packs);
}

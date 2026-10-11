package dev.marlondvg.mc_deck_api.application.port.in;

import dev.marlondvg.mc_deck_api.domain.OwnedPacks;

import java.util.Collection;
import java.util.UUID;

public interface ReplaceCollectionUseCase {

	OwnedPacks replaceCollection(UUID ownerId, Collection<String> packCodes);
}

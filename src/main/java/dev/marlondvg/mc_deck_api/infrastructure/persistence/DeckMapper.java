package dev.marlondvg.mc_deck_api.infrastructure.persistence;

import dev.marlondvg.mc_deck_api.domain.Deck;

import java.util.Map;
import java.util.TreeMap;

final class DeckMapper {

	private DeckMapper() {
	}

	static DeckJpaEntity toEntity(Deck deck) {
		return new DeckJpaEntity(deck.getId(), deck.getOwnerId(), deck.getName(), deck.getHeroCode(),
				deck.getAspects(), deck.getSlots(), deck.getCreatedAt(), deck.getUpdatedAt());
	}

	static Deck toDomain(DeckJpaEntity entity) {
		// Sorted by card code so responses are stable whatever order the database returns.
		Map<String, Integer> slots = new TreeMap<>(entity.getSlots());
		return Deck.restore(entity.getId(), entity.getOwnerId(), entity.getName(), entity.getHeroCode(),
				entity.getAspects(), slots, entity.getCreatedAt(), entity.getUpdatedAt());
	}
}

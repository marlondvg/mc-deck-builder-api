package dev.marlondvg.mc_deck_api.infrastructure.web.dto;

import dev.marlondvg.mc_deck_api.domain.Deck;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DeckResponse(
		UUID id,
		String name,
		String heroCode,
		List<String> aspects,
		Map<String, Integer> slots,
		Instant createdAt,
		Instant updatedAt) {

	public static DeckResponse from(Deck deck) {
		return new DeckResponse(deck.getId(), deck.getName(), deck.getHeroCode(), deck.getAspects(),
				deck.getSlots(), deck.getCreatedAt(), deck.getUpdatedAt());
	}
}

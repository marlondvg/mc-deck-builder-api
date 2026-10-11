package dev.marlondvg.mc_deck_api.infrastructure.web.dto;

import dev.marlondvg.mc_deck_api.domain.OwnedPacks;

import java.util.List;

public record CollectionResponse(List<String> packs) {

	public static CollectionResponse from(OwnedPacks packs) {
		return new CollectionResponse(List.copyOf(packs.codes()));
	}
}

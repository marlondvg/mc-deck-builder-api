package dev.marlondvg.mc_deck_api.infrastructure.web.dto;

import dev.marlondvg.mc_deck_api.application.port.in.DeckContents;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/**
 * Deck as sent by the client. Detailed checks (codes, aspects, copies) are domain rules in {@code Deck}.
 */
public record DeckRequest(
		@NotBlank @Size(max = 100) String name,
		@NotBlank @Size(max = 20) String heroCode,
		@NotEmpty @Size(max = 4) List<String> aspects,
		@NotNull @Size(max = 200) Map<String, Integer> slots) {

	public DeckContents toContents() {
		return new DeckContents(name, heroCode, aspects, slots);
	}
}

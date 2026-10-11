package dev.marlondvg.mc_deck_api.application.port.in;

import java.util.List;
import java.util.Map;

/**
 * The editable part of a deck, as sent by the client.
 */
public record DeckContents(String name, String heroCode, List<String> aspects, Map<String, Integer> slots) {
}

package dev.marlondvg.mc_deck_api.domain;

import dev.marlondvg.mc_deck_api.domain.exception.InvalidDeckException;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * A user's deck. Only checks that the data is well formed: deck-building rules
 * (deck size, copy limits per hero and aspect) depend on card data and are
 * checked by the frontend, so a deck being built can be saved at any point.
 */
public final class Deck {

	public static final int MAX_NAME_LENGTH = 100;
	public static final int MAX_ASPECTS = 4;
	public static final int MAX_COPIES = 9;
	public static final int MAX_DIFFERENT_CARDS = 200;
	public static final int MAX_CARDS = 200;
	public static final Set<String> ASPECTS = Set.of("aggression", "justice", "leadership", "protection", "pool");
	private static final Pattern CARD_CODE = Pattern.compile("^[a-z0-9]{1,20}$");

	private final UUID id;
	private final UUID ownerId;
	private final Instant createdAt;
	private String name;
	private String heroCode;
	private List<String> aspects;
	private Map<String, Integer> slots;
	private Instant updatedAt;

	private Deck(UUID id, UUID ownerId, String name, String heroCode, List<String> aspects,
			Map<String, Integer> slots, Instant createdAt, Instant updatedAt) {
		this.id = requireNonNull(id, "id");
		this.ownerId = requireNonNull(ownerId, "ownerId");
		this.createdAt = requireNonNull(createdAt, "createdAt");
		setContents(name, heroCode, aspects, slots);
		this.updatedAt = requireNonNull(updatedAt, "updatedAt");
	}

	public static Deck create(UUID ownerId, String name, String heroCode, List<String> aspects,
			Map<String, Integer> slots, Instant now) {
		return new Deck(UUID.randomUUID(), ownerId, name, heroCode, aspects, slots, now, now);
	}

	public static Deck restore(UUID id, UUID ownerId, String name, String heroCode, List<String> aspects,
			Map<String, Integer> slots, Instant createdAt, Instant updatedAt) {
		return new Deck(id, ownerId, name, heroCode, aspects, slots, createdAt, updatedAt);
	}

	/**
	 * Replaces the deck contents. Owner, id and creation date never change.
	 */
	public void update(String name, String heroCode, List<String> aspects, Map<String, Integer> slots,
			Instant now) {
		setContents(name, heroCode, aspects, slots);
		this.updatedAt = requireNonNull(now, "updatedAt");
	}

	// Everything is validated before any field changes, so a rejected update leaves the deck as it was.
	private void setContents(String name, String heroCode, List<String> aspects, Map<String, Integer> slots) {
		String validName = validName(name);
		String validHeroCode = validCardCode(heroCode, "heroCode");
		List<String> validAspects = validAspects(aspects);
		Map<String, Integer> validSlots = validSlots(slots);
		this.name = validName;
		this.heroCode = validHeroCode;
		this.aspects = validAspects;
		this.slots = validSlots;
	}

	private static String validName(String name) {
		if (name == null || name.isBlank()) {
			throw new InvalidDeckException("name is required");
		}
		String trimmed = name.strip();
		if (trimmed.length() > MAX_NAME_LENGTH) {
			throw new InvalidDeckException("name must be at most " + MAX_NAME_LENGTH + " characters");
		}
		return trimmed;
	}

	private static String validCardCode(String code, String field) {
		if (code == null || !CARD_CODE.matcher(code).matches()) {
			throw new InvalidDeckException(field + " is not a valid card code: " + code);
		}
		return code;
	}

	private static List<String> validAspects(List<String> aspects) {
		if (aspects == null || aspects.isEmpty()) {
			throw new InvalidDeckException("at least one aspect is required");
		}
		Set<String> unique = new LinkedHashSet<>();
		for (String aspect : aspects) {
			if (aspect == null || !ASPECTS.contains(aspect)) {
				throw new InvalidDeckException("unknown aspect: " + aspect);
			}
			if (!unique.add(aspect)) {
				throw new InvalidDeckException("aspect repeated: " + aspect);
			}
		}
		if (unique.size() > MAX_ASPECTS) {
			throw new InvalidDeckException("at most " + MAX_ASPECTS + " aspects");
		}
		return List.copyOf(unique);
	}

	private static Map<String, Integer> validSlots(Map<String, Integer> slots) {
		if (slots == null) {
			throw new InvalidDeckException("slots are required");
		}
		if (slots.size() > MAX_DIFFERENT_CARDS) {
			throw new InvalidDeckException("at most " + MAX_DIFFERENT_CARDS + " different cards");
		}
		Map<String, Integer> copy = new LinkedHashMap<>();
		int total = 0;
		for (Map.Entry<String, Integer> slot : slots.entrySet()) {
			String code = validCardCode(slot.getKey(), "card code");
			Integer copies = slot.getValue();
			if (copies == null || copies < 1 || copies > MAX_COPIES) {
				throw new InvalidDeckException("copies of " + code + " must be 1 to " + MAX_COPIES);
			}
			total += copies;
			copy.put(code, copies);
		}
		if (total > MAX_CARDS) {
			throw new InvalidDeckException("at most " + MAX_CARDS + " cards in total");
		}
		return Collections.unmodifiableMap(copy);
	}

	private static <T> T requireNonNull(T value, String field) {
		if (value == null) {
			throw new InvalidDeckException(field + " is required");
		}
		return value;
	}

	public UUID getId() {
		return id;
	}

	public UUID getOwnerId() {
		return ownerId;
	}

	public String getName() {
		return name;
	}

	public String getHeroCode() {
		return heroCode;
	}

	public List<String> getAspects() {
		return aspects;
	}

	/**
	 * Card code to copies; never contains 0 copies.
	 */
	public Map<String, Integer> getSlots() {
		return slots;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Deck other && id.equals(other.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return "Deck[id=" + id + ", heroCode=" + heroCode + "]";
	}
}

package dev.marlondvg.mc_deck_api.infrastructure.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "decks")
public class DeckJpaEntity {

	@Id
	private UUID id;

	@Column(name = "owner_id", nullable = false, updatable = false)
	private UUID ownerId;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(name = "hero_code", nullable = false, length = 20)
	private String heroCode;

	// An indexed list (not a bag), so it can be fetched eagerly together with the slots.
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "deck_aspects", joinColumns = @JoinColumn(name = "deck_id"))
	@OrderColumn(name = "position")
	@Column(name = "aspect", nullable = false, length = 20)
	private List<String> aspects = new ArrayList<>();

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "deck_slots", joinColumns = @JoinColumn(name = "deck_id"))
	@MapKeyColumn(name = "card_code", length = 20)
	@Column(name = "quantity", nullable = false)
	private Map<String, Integer> slots = new HashMap<>();

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected DeckJpaEntity() {
	}

	DeckJpaEntity(UUID id, UUID ownerId, String name, String heroCode, List<String> aspects,
			Map<String, Integer> slots, Instant createdAt, Instant updatedAt) {
		this.id = id;
		this.ownerId = ownerId;
		this.name = name;
		this.heroCode = heroCode;
		this.aspects = new ArrayList<>(aspects);
		this.slots = new HashMap<>(slots);
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	UUID getId() {
		return id;
	}

	UUID getOwnerId() {
		return ownerId;
	}

	String getName() {
		return name;
	}

	String getHeroCode() {
		return heroCode;
	}

	List<String> getAspects() {
		return aspects;
	}

	Map<String, Integer> getSlots() {
		return slots;
	}

	Instant getCreatedAt() {
		return createdAt;
	}

	Instant getUpdatedAt() {
		return updatedAt;
	}
}

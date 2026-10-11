package dev.marlondvg.mc_deck_api.infrastructure.persistence;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "owned_packs")
public class OwnedPackJpaEntity {

	@EmbeddedId
	private OwnedPackId id;

	protected OwnedPackJpaEntity() {
	}

	OwnedPackJpaEntity(UUID userId, String packCode) {
		this.id = new OwnedPackId(userId, packCode);
	}

	String getPackCode() {
		return id.getPackCode();
	}
}

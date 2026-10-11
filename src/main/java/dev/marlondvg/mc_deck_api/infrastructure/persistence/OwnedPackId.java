package dev.marlondvg.mc_deck_api.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class OwnedPackId implements Serializable {

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "pack_code", nullable = false, length = 50)
	private String packCode;

	protected OwnedPackId() {
	}

	OwnedPackId(UUID userId, String packCode) {
		this.userId = userId;
		this.packCode = packCode;
	}

	String getPackCode() {
		return packCode;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof OwnedPackId other && userId.equals(other.userId) && packCode.equals(other.packCode);
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId, packCode);
	}
}

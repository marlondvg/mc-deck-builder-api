package dev.marlondvg.mc_deck_api.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserJpaEntity {

	@Id
	private UUID id;

	@Column(nullable = false, unique = true, length = 30)
	private String username;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected UserJpaEntity() {
	}

	UserJpaEntity(UUID id, String username, String passwordHash, Instant createdAt) {
		this.id = id;
		this.username = username;
		this.passwordHash = passwordHash;
		this.createdAt = createdAt;
	}

	UUID getId() {
		return id;
	}

	String getUsername() {
		return username;
	}

	String getPasswordHash() {
		return passwordHash;
	}

	Instant getCreatedAt() {
		return createdAt;
	}
}

package dev.marlondvg.mc_deck_api.domain;

import dev.marlondvg.mc_deck_api.domain.exception.InvalidUserException;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public final class User {

	public static final int MIN_USERNAME_LENGTH = 3;
	public static final int MAX_USERNAME_LENGTH = 30;
	// Lower-case letters, digits, "_", "." and "-" (checked after lower-casing).
	private static final Pattern USERNAME = Pattern.compile("^[a-z0-9_.-]+$");

	private final UUID id;
	private final String username;
	private final String passwordHash;
	private final Instant createdAt;

	private User(UUID id, String username, String passwordHash, Instant createdAt) {
		this.id = requireNonNull(id, "id");
		this.username = normalizeUsername(username);
		this.passwordHash = requireNonNull(passwordHash, "passwordHash");
		this.createdAt = requireNonNull(createdAt, "createdAt");
	}

	public static User register(String username, String passwordHash, Instant createdAt) {
		return new User(UUID.randomUUID(), username, passwordHash, createdAt);
	}

	public static User restore(UUID id, String username, String passwordHash, Instant createdAt) {
		return new User(id, username, passwordHash, createdAt);
	}

	/**
	 * Trims and lower-cases a username so "Ann" and "ann" are the same account.
	 */
	public static String normalizeUsername(String username) {
		if (username == null || username.isBlank()) {
			throw new InvalidUserException("username is required");
		}
		String normalized = username.strip().toLowerCase(Locale.ROOT);
		if (normalized.length() < MIN_USERNAME_LENGTH || normalized.length() > MAX_USERNAME_LENGTH) {
			throw new InvalidUserException("username must be " + MIN_USERNAME_LENGTH + " to "
					+ MAX_USERNAME_LENGTH + " characters");
		}
		if (!USERNAME.matcher(normalized).matches()) {
			throw new InvalidUserException("username may only contain letters, digits, \"_\", \".\" and \"-\"");
		}
		return normalized;
	}

	private static <T> T requireNonNull(T value, String field) {
		if (value == null) {
			throw new InvalidUserException(field + " is required");
		}
		return value;
	}

	public UUID getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof User other && id.equals(other.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return "User[id=" + id + "]";
	}
}

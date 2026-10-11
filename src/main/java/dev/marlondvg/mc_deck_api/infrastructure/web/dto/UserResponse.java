package dev.marlondvg.mc_deck_api.infrastructure.web.dto;

import dev.marlondvg.mc_deck_api.domain.User;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String username, Instant createdAt) {

	public static UserResponse from(User user) {
		return new UserResponse(user.getId(), user.getUsername(), user.getCreatedAt());
	}
}

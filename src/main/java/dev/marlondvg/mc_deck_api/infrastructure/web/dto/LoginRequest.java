package dev.marlondvg.mc_deck_api.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String username, @NotBlank String password) {

	@Override
	public String toString() {
		return "LoginRequest[username=" + username + ", password=***]";
	}
}

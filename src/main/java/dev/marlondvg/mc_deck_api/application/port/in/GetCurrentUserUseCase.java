package dev.marlondvg.mc_deck_api.application.port.in;

import dev.marlondvg.mc_deck_api.domain.User;

import java.util.UUID;

public interface GetCurrentUserUseCase {

	User getCurrentUser(UUID userId);
}

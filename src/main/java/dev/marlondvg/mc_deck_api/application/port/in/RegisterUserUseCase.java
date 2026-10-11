package dev.marlondvg.mc_deck_api.application.port.in;

import dev.marlondvg.mc_deck_api.domain.User;

public interface RegisterUserUseCase {

	User register(RegisterUserCommand command);
}

package dev.marlondvg.mc_deck_api.application.port.in;

import dev.marlondvg.mc_deck_api.application.port.out.AccessToken;

public interface LoginUseCase {

	AccessToken login(LoginCommand command);
}

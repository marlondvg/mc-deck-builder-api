package dev.marlondvg.mc_deck_api.application.port.out;

import dev.marlondvg.mc_deck_api.domain.User;

public interface TokenIssuer {

	AccessToken issueFor(User user);
}

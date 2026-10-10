package dev.marlondvg.mc_deck_api.domain.exception;

public class UsernameAlreadyUsedException extends DomainException {

	public UsernameAlreadyUsedException() {
		super("An account with this username already exists");
	}
}

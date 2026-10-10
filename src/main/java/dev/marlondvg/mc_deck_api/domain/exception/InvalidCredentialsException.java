package dev.marlondvg.mc_deck_api.domain.exception;

public class InvalidCredentialsException extends DomainException {

	public InvalidCredentialsException() {
		super("Invalid username or password");
	}
}

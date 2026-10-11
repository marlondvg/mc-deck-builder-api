package dev.marlondvg.mc_deck_api.domain.exception;

/**
 * The token is valid but its user no longer exists.
 */
public class UnknownUserException extends DomainException {

	public UnknownUserException() {
		super("User no longer exists");
	}
}

package dev.marlondvg.mc_deck_api.domain.exception;

public abstract class DomainException extends RuntimeException {

	protected DomainException(String message) {
		super(message);
	}
}

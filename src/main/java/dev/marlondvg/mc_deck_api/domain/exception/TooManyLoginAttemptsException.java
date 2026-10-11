package dev.marlondvg.mc_deck_api.domain.exception;

import java.time.Duration;

public class TooManyLoginAttemptsException extends DomainException {

	private final Duration retryAfter;

	public TooManyLoginAttemptsException(Duration retryAfter) {
		super("Too many failed login attempts. Try again later.");
		this.retryAfter = retryAfter;
	}

	public Duration getRetryAfter() {
		return retryAfter;
	}
}

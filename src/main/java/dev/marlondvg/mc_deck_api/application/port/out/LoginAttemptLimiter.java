package dev.marlondvg.mc_deck_api.application.port.out;

import java.time.Duration;
import java.util.Optional;

/**
 * Tracks failed logins per username and per client IP. {@code username} is the
 * normalized username, or {@code null} when the given username was malformed.
 */
public interface LoginAttemptLimiter {

	/**
	 * Returns how long until login is allowed again, or empty if it is allowed now.
	 */
	Optional<Duration> retryAfter(String username, String clientIp);

	void recordFailure(String username, String clientIp);

	void recordSuccess(String username);
}

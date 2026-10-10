package dev.marlondvg.mc_deck_api.infrastructure.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryLoginAttemptLimiterTest {

	private static final String USERNAME = "ann";
	private static final String IP = "203.0.113.7";

	private final MutableClock clock = new MutableClock(Instant.parse("2026-04-01T12:00:00Z"));
	private final InMemoryLoginAttemptLimiter limiter =
			new InMemoryLoginAttemptLimiter(new LoginLimitProperties(5, 20, Duration.ofMinutes(15)), clock);

	private void fail(String username, String ip, int times) {
		for (int i = 0; i < times; i++) {
			limiter.recordFailure(username, ip);
		}
	}

	@Test
	void shouldAllowUpToFourFailuresPerUsername() {
		fail(USERNAME, IP, 4);

		assertThat(limiter.retryAfter(USERNAME, IP)).isEmpty();
	}

	@Test
	void shouldBlockUsernameAfterFiveFailuresUntilWindowEnds() {
		fail(USERNAME, IP, 5);
		clock.advance(Duration.ofMinutes(5));

		assertThat(limiter.retryAfter(USERNAME, IP)).contains(Duration.ofMinutes(10));
		assertThat(limiter.retryAfter(USERNAME, "198.51.100.1")).contains(Duration.ofMinutes(10));
	}

	@Test
	void shouldAllowAgainOnceWindowHasEnded() {
		fail(USERNAME, IP, 5);
		clock.advance(Duration.ofMinutes(15));

		assertThat(limiter.retryAfter(USERNAME, IP)).isEmpty();
	}

	@Test
	void shouldStartNewWindowAfterExpiry() {
		fail(USERNAME, IP, 4);
		clock.advance(Duration.ofMinutes(16));
		fail(USERNAME, IP, 4);

		assertThat(limiter.retryAfter(USERNAME, IP)).isEmpty();
	}

	@Test
	void shouldNotAffectOtherUsernames() {
		fail(USERNAME, IP, 5);

		assertThat(limiter.retryAfter("bob", "198.51.100.1")).isEmpty();
	}

	@Test
	void shouldBlockIpAfterTwentyFailuresAcrossUsernames() {
		for (int i = 0; i < 20; i++) {
			limiter.recordFailure("user" + i, IP);
		}

		assertThat(limiter.retryAfter("new", IP)).isPresent();
		assertThat(limiter.retryAfter("new", "198.51.100.1")).isEmpty();
	}

	@Test
	void shouldCountMalformedUsernameOnlyAgainstIp() {
		fail(null, IP, 20);

		assertThat(limiter.retryAfter(null, IP)).isPresent();
		assertThat(limiter.retryAfter(USERNAME, "198.51.100.1")).isEmpty();
	}

	@Test
	void shouldReturnLongestWaitWhenUsernameAndIpAreBothBlocked() {
		for (int i = 0; i < 20; i++) {
			limiter.recordFailure("user" + i, IP);
		}
		clock.advance(Duration.ofMinutes(10));
		fail(USERNAME, "198.51.100.1", 5);

		assertThat(limiter.retryAfter(USERNAME, IP)).contains(Duration.ofMinutes(15));
	}

	@Test
	void shouldResetUsernameCounterOnSuccess() {
		fail(USERNAME, IP, 4);
		limiter.recordSuccess(USERNAME);
		fail(USERNAME, IP, 4);

		assertThat(limiter.retryAfter(USERNAME, IP)).isEmpty();
	}

	@Test
	void shouldPurgeExpiredWindowsWhenManyKeysAreTracked() {
		for (int i = 0; i <= InMemoryLoginAttemptLimiter.PURGE_THRESHOLD; i++) {
			limiter.recordFailure("user" + i, null);
		}
		clock.advance(Duration.ofMinutes(16));

		limiter.recordFailure(USERNAME, IP);

		assertThat(limiter.trackedKeys()).isEqualTo(2);
	}

	private static final class MutableClock extends Clock {

		private Instant now;

		MutableClock(Instant now) {
			this.now = now;
		}

		void advance(Duration duration) {
			now = now.plus(duration);
		}

		@Override
		public Instant instant() {
			return now;
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}
	}
}

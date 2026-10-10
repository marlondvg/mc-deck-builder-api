package dev.marlondvg.mc_deck_api.domain;

import dev.marlondvg.mc_deck_api.domain.exception.InvalidUserException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

	private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");

	@Test
	void shouldRegisterUserWithNormalizedUsername() {
		User user = User.register("  Ann ", "hash", CREATED_AT);

		assertThat(user.getId()).isNotNull();
		assertThat(user.getUsername()).isEqualTo("ann");
		assertThat(user.getPasswordHash()).isEqualTo("hash");
		assertThat(user.getCreatedAt()).isEqualTo(CREATED_AT);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"   ", "an", "ann smith", "ann@home", "añn", "ann!"})
	void shouldRejectInvalidUsername(String username) {
		assertThatThrownBy(() -> User.register(username, "hash", CREATED_AT))
				.isInstanceOf(InvalidUserException.class);
	}

	@Test
	void shouldRejectTooLongUsername() {
		String username = "a".repeat(User.MAX_USERNAME_LENGTH + 1);

		assertThatThrownBy(() -> User.register(username, "hash", CREATED_AT))
				.isInstanceOf(InvalidUserException.class);
	}

	@Test
	void shouldAcceptUsernameWithAllowedSymbols() {
		assertThat(User.register("Peter_Parker.616-x", "hash", CREATED_AT).getUsername())
				.isEqualTo("peter_parker.616-x");
	}

	@Test
	void shouldRejectMissingPasswordHash() {
		assertThatThrownBy(() -> User.register("ann", null, CREATED_AT))
				.isInstanceOf(InvalidUserException.class);
	}

	@Test
	void shouldNotExposePasswordHashInToString() {
		User user = User.register("ann", "secret-hash", CREATED_AT);

		assertThat(user.toString()).doesNotContain("secret-hash").doesNotContain("ann");
	}
}

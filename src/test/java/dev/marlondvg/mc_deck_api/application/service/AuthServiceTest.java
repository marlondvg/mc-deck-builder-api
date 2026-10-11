package dev.marlondvg.mc_deck_api.application.service;

import dev.marlondvg.mc_deck_api.application.port.in.LoginCommand;
import dev.marlondvg.mc_deck_api.application.port.in.RegisterUserCommand;
import dev.marlondvg.mc_deck_api.application.port.out.AccessToken;
import dev.marlondvg.mc_deck_api.application.port.out.LoginAttemptLimiter;
import dev.marlondvg.mc_deck_api.application.port.out.PasswordHasher;
import dev.marlondvg.mc_deck_api.application.port.out.TokenIssuer;
import dev.marlondvg.mc_deck_api.application.port.out.UserRepository;
import dev.marlondvg.mc_deck_api.domain.User;
import dev.marlondvg.mc_deck_api.domain.exception.UsernameAlreadyUsedException;
import dev.marlondvg.mc_deck_api.domain.exception.InvalidCredentialsException;
import dev.marlondvg.mc_deck_api.domain.exception.InvalidPasswordException;
import dev.marlondvg.mc_deck_api.domain.exception.InvalidUserException;
import dev.marlondvg.mc_deck_api.domain.exception.TooManyLoginAttemptsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	private static final Instant NOW = Instant.parse("2026-04-01T12:00:00Z");
	private static final String PASSWORD = "correct horse battery";
	private static final String IP = "203.0.113.7";

	@Mock
	private UserRepository userRepository;
	@Mock
	private PasswordHasher passwordHasher;
	@Mock
	private TokenIssuer tokenIssuer;
	@Mock
	private LoginAttemptLimiter loginAttemptLimiter;

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(userRepository, passwordHasher, tokenIssuer, loginAttemptLimiter,
				Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Nested
	class Register {

		@Test
		void shouldSaveUserWithNormalizedUsernameAndHashedPassword() {
			when(userRepository.existsByUsername("ann")).thenReturn(false);
			when(passwordHasher.hash(PASSWORD)).thenReturn("hashed");
			when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

			User user = authService.register(new RegisterUserCommand(" Ann ", PASSWORD));

			assertThat(user.getUsername()).isEqualTo("ann");
			assertThat(user.getPasswordHash()).isEqualTo("hashed");
			assertThat(user.getCreatedAt()).isEqualTo(NOW);
			verify(userRepository).save(user);
		}

		@Test
		void shouldRejectUsernameThatIsAlreadyUsed() {
			when(userRepository.existsByUsername("ann")).thenReturn(true);

			assertThatThrownBy(() -> authService.register(new RegisterUserCommand("ANN", PASSWORD)))
					.isInstanceOf(UsernameAlreadyUsedException.class);

			verify(userRepository, never()).save(any());
			verifyNoInteractions(passwordHasher);
		}

		@Test
		void shouldRejectInvalidUsernameBeforeHashing() {
			assertThatThrownBy(() -> authService.register(new RegisterUserCommand("not a username!", PASSWORD)))
					.isInstanceOf(InvalidUserException.class);

			verifyNoInteractions(userRepository, passwordHasher);
		}

		@Test
		void shouldRejectWeakPasswordBeforeHashing() {
			assertThatThrownBy(() -> authService.register(new RegisterUserCommand("ann", "short")))
					.isInstanceOf(InvalidPasswordException.class);

			verifyNoInteractions(userRepository, passwordHasher);
		}

		@Test
		void shouldNotShowPasswordInCommandToString() {
			assertThat(new RegisterUserCommand("ann", PASSWORD).toString()).doesNotContain(PASSWORD);
			assertThat(new LoginCommand("ann", PASSWORD, IP).toString()).doesNotContain(PASSWORD);
		}
	}

	@Nested
	class Login {

		private final User user = User.restore(java.util.UUID.randomUUID(), "ann", "stored-hash",
				NOW.minusSeconds(3600));

		@Test
		void shouldIssueTokenForValidCredentials() {
			AccessToken token = new AccessToken("jwt", Duration.ofHours(1));
			when(userRepository.findByUsername("ann")).thenReturn(Optional.of(user));
			when(passwordHasher.matches(PASSWORD, "stored-hash")).thenReturn(true);
			when(tokenIssuer.issueFor(user)).thenReturn(token);

			assertThat(authService.login(new LoginCommand(" ANN", PASSWORD, IP))).isEqualTo(token);
		}

		@Test
		void shouldRejectWrongPassword() {
			when(userRepository.findByUsername("ann")).thenReturn(Optional.of(user));
			when(passwordHasher.matches("wrong password", "stored-hash")).thenReturn(false);

			assertThatThrownBy(() -> authService.login(new LoginCommand("ann", "wrong password", IP)))
					.isInstanceOf(InvalidCredentialsException.class)
					.hasMessage("Invalid username or password");

			verifyNoInteractions(tokenIssuer);
		}

		@Test
		void shouldRejectUnknownUsernameWithSameErrorAndStillHash() {
			when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

			assertThatThrownBy(() -> authService.login(new LoginCommand("nobody", PASSWORD, IP)))
					.isInstanceOf(InvalidCredentialsException.class)
					.hasMessage("Invalid username or password");

			verify(passwordHasher).hash(PASSWORD);
			verifyNoInteractions(tokenIssuer);
		}

		@Test
		void shouldRejectMalformedUsernameLikeUnknownUsername() {
			assertThatThrownBy(() -> authService.login(new LoginCommand("not a username!", PASSWORD, IP)))
					.isInstanceOf(InvalidCredentialsException.class);

			verify(passwordHasher).hash(PASSWORD);
			verifyNoInteractions(userRepository, tokenIssuer);
		}

		@Test
		void shouldRejectPasswordLongerThan72BytesWithoutHashing() {
			assertThatThrownBy(() -> authService.login(new LoginCommand("ann", "a".repeat(73), IP)))
					.isInstanceOf(InvalidCredentialsException.class);

			verifyNoInteractions(userRepository, passwordHasher, tokenIssuer);
		}

		@Test
		void shouldRefuseBlockedLoginBeforeHashing() {
			when(loginAttemptLimiter.retryAfter("ann", IP)).thenReturn(Optional.of(Duration.ofMinutes(3)));

			assertThatThrownBy(() -> authService.login(new LoginCommand("Ann", PASSWORD, IP)))
					.isInstanceOfSatisfying(TooManyLoginAttemptsException.class,
							e -> assertThat(e.getRetryAfter()).isEqualTo(Duration.ofMinutes(3)));

			verifyNoInteractions(userRepository, passwordHasher, tokenIssuer);
			verify(loginAttemptLimiter, never()).recordFailure(any(), any());
		}

		@Test
		void shouldRecordSuccessAndNoFailureForValidCredentials() {
			when(userRepository.findByUsername("ann")).thenReturn(Optional.of(user));
			when(passwordHasher.matches(PASSWORD, "stored-hash")).thenReturn(true);
			when(tokenIssuer.issueFor(user)).thenReturn(new AccessToken("jwt", Duration.ofHours(1)));

			authService.login(new LoginCommand("ann", PASSWORD, IP));

			verify(loginAttemptLimiter).recordSuccess("ann");
			verify(loginAttemptLimiter, never()).recordFailure(any(), any());
		}

		@Test
		void shouldRecordFailureForWrongPassword() {
			when(userRepository.findByUsername("ann")).thenReturn(Optional.of(user));
			when(passwordHasher.matches("wrong password", "stored-hash")).thenReturn(false);

			assertThatThrownBy(() -> authService.login(new LoginCommand("ann", "wrong password", IP)))
					.isInstanceOf(InvalidCredentialsException.class);

			verify(loginAttemptLimiter).recordFailure("ann", IP);
			verify(loginAttemptLimiter, never()).recordSuccess(any());
		}

		@Test
		void shouldCountMalformedUsernameOnlyAgainstIp() {
			assertThatThrownBy(() -> authService.login(new LoginCommand("not a username!", PASSWORD, IP)))
					.isInstanceOf(InvalidCredentialsException.class);

			verify(loginAttemptLimiter).retryAfter(null, IP);
			verify(loginAttemptLimiter).recordFailure(null, IP);
		}
	}
}

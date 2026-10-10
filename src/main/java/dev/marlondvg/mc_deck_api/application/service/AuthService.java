package dev.marlondvg.mc_deck_api.application.service;

import dev.marlondvg.mc_deck_api.application.port.in.LoginCommand;
import dev.marlondvg.mc_deck_api.application.port.in.LoginUseCase;
import dev.marlondvg.mc_deck_api.application.port.in.RegisterUserCommand;
import dev.marlondvg.mc_deck_api.application.port.in.RegisterUserUseCase;
import dev.marlondvg.mc_deck_api.application.port.out.AccessToken;
import dev.marlondvg.mc_deck_api.application.port.out.LoginAttemptLimiter;
import dev.marlondvg.mc_deck_api.application.port.out.PasswordHasher;
import dev.marlondvg.mc_deck_api.application.port.out.TokenIssuer;
import dev.marlondvg.mc_deck_api.application.port.out.UserRepository;
import dev.marlondvg.mc_deck_api.domain.PasswordPolicy;
import dev.marlondvg.mc_deck_api.domain.User;
import dev.marlondvg.mc_deck_api.domain.exception.UsernameAlreadyUsedException;
import dev.marlondvg.mc_deck_api.domain.exception.InvalidCredentialsException;
import dev.marlondvg.mc_deck_api.domain.exception.InvalidUserException;
import dev.marlondvg.mc_deck_api.domain.exception.TooManyLoginAttemptsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
@Transactional
public class AuthService implements RegisterUserUseCase, LoginUseCase {

	private final UserRepository userRepository;
	private final PasswordHasher passwordHasher;
	private final TokenIssuer tokenIssuer;
	private final LoginAttemptLimiter loginAttemptLimiter;
	private final Clock clock;

	public AuthService(UserRepository userRepository, PasswordHasher passwordHasher, TokenIssuer tokenIssuer,
			LoginAttemptLimiter loginAttemptLimiter, Clock clock) {
		this.userRepository = userRepository;
		this.passwordHasher = passwordHasher;
		this.tokenIssuer = tokenIssuer;
		this.loginAttemptLimiter = loginAttemptLimiter;
		this.clock = clock;
	}

	@Override
	public User register(RegisterUserCommand command) {
		String username = User.normalizeUsername(command.username());
		PasswordPolicy.validate(command.password());
		if (userRepository.existsByUsername(username)) {
			throw new UsernameAlreadyUsedException();
		}
		User user = User.register(username, passwordHasher.hash(command.password()), Instant.now(clock));
		return userRepository.save(user);
	}

	@Override
	@Transactional(readOnly = true)
	public AccessToken login(LoginCommand command) {
		String username = normalizeOrNull(command.username());
		// Checked before any hashing, so blocked attempts cost no BCrypt time.
		loginAttemptLimiter.retryAfter(username, command.clientIp()).ifPresent(retryAfter -> {
			throw new TooManyLoginAttemptsException(retryAfter);
		});
		try {
			User user = authenticate(username, command.password());
			loginAttemptLimiter.recordSuccess(username);
			return tokenIssuer.issueFor(user);
		} catch (InvalidCredentialsException e) {
			loginAttemptLimiter.recordFailure(username, command.clientIp());
			throw e;
		}
	}

	private User authenticate(String username, String password) {
		if (password == null || PasswordPolicy.exceedsMaxBytes(password)) {
			throw new InvalidCredentialsException();
		}
		Optional<User> user = username == null ? Optional.empty() : userRepository.findByUsername(username);
		if (user.isEmpty()) {
			// Spend the same time as a real check so response time does not reveal unknown usernames.
			passwordHasher.hash(password);
			throw new InvalidCredentialsException();
		}
		if (!passwordHasher.matches(password, user.get().getPasswordHash())) {
			throw new InvalidCredentialsException();
		}
		return user.get();
	}

	/**
	 * Returns the normalized username, or {@code null} if it is malformed. A malformed
	 * username cannot belong to an account, so login answers like any unknown username.
	 */
	private static String normalizeOrNull(String rawUsername) {
		try {
			return User.normalizeUsername(rawUsername);
		} catch (InvalidUserException invalidUsername) {
			return null;
		}
	}
}

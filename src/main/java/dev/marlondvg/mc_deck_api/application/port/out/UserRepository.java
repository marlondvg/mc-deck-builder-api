package dev.marlondvg.mc_deck_api.application.port.out;

import dev.marlondvg.mc_deck_api.domain.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

	/**
	 * Saves a new user.
	 *
	 * @throws dev.marlondvg.mc_deck_api.domain.exception.UsernameAlreadyUsedException if the username is taken
	 */
	User save(User user);

	/**
	 * Finds a user by an username that is already normalized.
	 */
	Optional<User> findByUsername(String username);

	boolean existsByUsername(String username);

	Optional<User> findById(UUID id);
}

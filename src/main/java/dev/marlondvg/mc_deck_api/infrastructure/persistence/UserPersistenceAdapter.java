package dev.marlondvg.mc_deck_api.infrastructure.persistence;

import dev.marlondvg.mc_deck_api.application.port.out.UserRepository;
import dev.marlondvg.mc_deck_api.domain.User;
import dev.marlondvg.mc_deck_api.domain.exception.UsernameAlreadyUsedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
class UserPersistenceAdapter implements UserRepository {

	private final SpringDataUserRepository repository;

	UserPersistenceAdapter(SpringDataUserRepository repository) {
		this.repository = repository;
	}

	@Override
	public User save(User user) {
		UserJpaEntity entity = new UserJpaEntity(user.getId(), user.getUsername(), user.getPasswordHash(),
				user.getCreatedAt());
		try {
			// Flush now so a duplicate username (unique constraint) fails here, not at commit.
			return toDomain(repository.saveAndFlush(entity));
		} catch (DataIntegrityViolationException e) {
			// The only constraint the domain cannot check itself is the unique username.
			throw new UsernameAlreadyUsedException();
		}
	}

	@Override
	public Optional<User> findByUsername(String username) {
		return repository.findByUsername(username).map(UserPersistenceAdapter::toDomain);
	}

	@Override
	public boolean existsByUsername(String username) {
		return repository.existsByUsername(username);
	}

	@Override
	public Optional<User> findById(UUID id) {
		return repository.findById(id).map(UserPersistenceAdapter::toDomain);
	}

	private static User toDomain(UserJpaEntity entity) {
		return User.restore(entity.getId(), entity.getUsername(), entity.getPasswordHash(), entity.getCreatedAt());
	}
}

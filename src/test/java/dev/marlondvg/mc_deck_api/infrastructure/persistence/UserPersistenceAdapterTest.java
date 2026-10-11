package dev.marlondvg.mc_deck_api.infrastructure.persistence;

import dev.marlondvg.mc_deck_api.domain.User;
import dev.marlondvg.mc_deck_api.domain.exception.UsernameAlreadyUsedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(UserPersistenceAdapter.class)
class UserPersistenceAdapterTest {

	private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");

	@Autowired
	private UserPersistenceAdapter adapter;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void shouldSaveAndFindUserByUsername() {
		User user = User.register("ann", "hashed", CREATED_AT);
		adapter.save(user);
		entityManager.clear();

		User found = adapter.findByUsername("ann").orElseThrow();

		assertThat(found.getId()).isEqualTo(user.getId());
		assertThat(found.getUsername()).isEqualTo("ann");
		assertThat(found.getPasswordHash()).isEqualTo("hashed");
		assertThat(found.getCreatedAt()).isEqualTo(CREATED_AT);
	}

	@Test
	void shouldReportWhetherUsernameExists() {
		adapter.save(User.register("ann", "hashed", CREATED_AT));

		assertThat(adapter.existsByUsername("ann")).isTrue();
		assertThat(adapter.existsByUsername("bob")).isFalse();
		assertThat(adapter.findByUsername("bob")).isEmpty();
	}

	@Test
	void shouldFindUserById() {
		User user = User.register("ann", "hashed", CREATED_AT);
		adapter.save(user);
		entityManager.clear();

		User found = adapter.findById(user.getId()).orElseThrow();

		assertThat(found.getUsername()).isEqualTo("ann");
		assertThat(adapter.findById(UUID.randomUUID())).isEmpty();
	}

	@Test
	void shouldTranslateDuplicateUsernameToDomainException() {
		adapter.save(User.register("ann", "hashed", CREATED_AT));

		assertThatThrownBy(() -> adapter.save(User.register("ann", "other", CREATED_AT)))
				.isInstanceOf(UsernameAlreadyUsedException.class);
	}
}

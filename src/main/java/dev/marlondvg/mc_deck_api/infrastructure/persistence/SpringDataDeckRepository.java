package dev.marlondvg.mc_deck_api.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataDeckRepository extends JpaRepository<DeckJpaEntity, UUID> {

	Optional<DeckJpaEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

	List<DeckJpaEntity> findAllByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);
}

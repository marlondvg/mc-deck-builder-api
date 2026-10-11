package dev.marlondvg.mc_deck_api.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface SpringDataOwnedPackRepository extends JpaRepository<OwnedPackJpaEntity, OwnedPackId> {

	@Query("select p from OwnedPackJpaEntity p where p.id.userId = :userId")
	List<OwnedPackJpaEntity> findAllByUserId(@Param("userId") UUID userId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from OwnedPackJpaEntity p where p.id.userId = :userId")
	void deleteAllByUserId(@Param("userId") UUID userId);
}

package dev.marlondvg.mc_deck_api.application.service;

import dev.marlondvg.mc_deck_api.application.port.out.CollectionRepository;
import dev.marlondvg.mc_deck_api.domain.OwnedPacks;
import dev.marlondvg.mc_deck_api.domain.exception.InvalidCollectionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectionServiceTest {

	private static final UUID OWNER_ID = UUID.randomUUID();

	@Mock
	private CollectionRepository collectionRepository;

	@InjectMocks
	private CollectionService collectionService;

	@Test
	void shouldReturnOwnersCollection() {
		OwnedPacks packs = OwnedPacks.of(List.of("core"));
		when(collectionRepository.findByOwnerId(OWNER_ID)).thenReturn(packs);

		assertThat(collectionService.getCollection(OWNER_ID)).isEqualTo(packs);
	}

	@Test
	void shouldReplaceCollectionWithNormalizedPacks() {
		OwnedPacks result = collectionService.replaceCollection(OWNER_ID, List.of("core", "cap", "core"));

		assertThat(result.codes()).containsExactly("cap", "core");
		verify(collectionRepository).replace(OWNER_ID, result);
	}

	@Test
	void shouldNotTouchRepositoryWhenPacksAreInvalid() {
		assertThatThrownBy(() -> collectionService.replaceCollection(OWNER_ID, List.of("Not Valid")))
				.isInstanceOf(InvalidCollectionException.class);
		verifyNoInteractions(collectionRepository);
	}
}

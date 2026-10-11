package dev.marlondvg.mc_deck_api.domain;

import dev.marlondvg.mc_deck_api.domain.exception.InvalidCollectionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OwnedPacksTest {

	@Test
	void shouldSortAndRemoveDuplicates() {
		assertThat(OwnedPacks.of(List.of("mut_gen", "core", "core", "cap")).codes())
				.containsExactly("cap", "core", "mut_gen");
	}

	@Test
	void shouldBeEmptyByDefault() {
		assertThat(OwnedPacks.empty().codes()).isEmpty();
		assertThat(OwnedPacks.of(List.of())).isEqualTo(OwnedPacks.empty());
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "Core", "core set", "core-set", "../x"})
	void shouldRejectInvalidPackCodes(String code) {
		assertThatThrownBy(() -> OwnedPacks.of(List.of("core", code)))
				.isInstanceOf(InvalidCollectionException.class);
	}

	@Test
	void shouldRejectNullListOrNullCode() {
		assertThatThrownBy(() -> OwnedPacks.of(null)).isInstanceOf(InvalidCollectionException.class);
		assertThatThrownBy(() -> OwnedPacks.of(Arrays.asList("core", null)))
				.isInstanceOf(InvalidCollectionException.class);
	}

	@Test
	void shouldRejectMoreThanMaxPacks() {
		List<String> codes = new ArrayList<>();
		for (int i = 0; i <= OwnedPacks.MAX_PACKS; i++) {
			codes.add("pack" + i);
		}
		assertThatThrownBy(() -> OwnedPacks.of(codes)).isInstanceOf(InvalidCollectionException.class);
	}
}

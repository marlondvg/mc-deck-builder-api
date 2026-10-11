package dev.marlondvg.mc_deck_api.infrastructure.web;

import dev.marlondvg.mc_deck_api.application.port.in.GetCollectionUseCase;
import dev.marlondvg.mc_deck_api.application.port.in.ReplaceCollectionUseCase;
import dev.marlondvg.mc_deck_api.domain.OwnedPacks;
import dev.marlondvg.mc_deck_api.domain.exception.InvalidCollectionException;
import dev.marlondvg.mc_deck_api.infrastructure.config.CorsConfig;
import dev.marlondvg.mc_deck_api.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CollectionController.class)
@Import({SecurityConfig.class, CorsConfig.class})
class CollectionControllerTest {

	private static final UUID OWNER_ID = UUID.randomUUID();

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GetCollectionUseCase getCollection;
	@MockitoBean
	private ReplaceCollectionUseCase replaceCollection;

	private static RequestPostProcessor owner() {
		return jwt().jwt(token -> token.subject(OWNER_ID.toString()));
	}

	@Test
	void shouldReturnCurrentUsersPacks() throws Exception {
		when(getCollection.getCollection(OWNER_ID)).thenReturn(OwnedPacks.of(List.of("core", "cap")));

		mockMvc.perform(get("/api/collection").with(owner()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.packs[0]").value("cap"))
				.andExpect(jsonPath("$.packs[1]").value("core"));
	}

	@Test
	void shouldReplaceCollection() throws Exception {
		when(replaceCollection.replaceCollection(OWNER_ID, List.of("core", "mut_gen")))
				.thenReturn(OwnedPacks.of(List.of("core", "mut_gen")));

		mockMvc.perform(put("/api/collection").with(owner()).contentType(MediaType.APPLICATION_JSON)
						.content("{\"packs\": [\"core\", \"mut_gen\"]}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.packs.length()").value(2));
	}

	@Test
	void shouldRejectMissingPacksAndInvalidCodes() throws Exception {
		mockMvc.perform(put("/api/collection").with(owner()).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(replaceCollection);

		when(replaceCollection.replaceCollection(eq(OWNER_ID), any()))
				.thenThrow(new InvalidCollectionException("invalid pack code: Core"));
		mockMvc.perform(put("/api/collection").with(owner()).contentType(MediaType.APPLICATION_JSON)
						.content("{\"packs\": [\"Core\"]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("invalid pack code: Core"));
	}
}

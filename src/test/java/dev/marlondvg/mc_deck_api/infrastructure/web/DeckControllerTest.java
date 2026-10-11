package dev.marlondvg.mc_deck_api.infrastructure.web;

import dev.marlondvg.mc_deck_api.application.port.in.CreateDeckUseCase;
import dev.marlondvg.mc_deck_api.application.port.in.DeckContents;
import dev.marlondvg.mc_deck_api.application.port.in.DeleteDeckUseCase;
import dev.marlondvg.mc_deck_api.application.port.in.GetDeckUseCase;
import dev.marlondvg.mc_deck_api.application.port.in.ListDecksUseCase;
import dev.marlondvg.mc_deck_api.application.port.in.UpdateDeckUseCase;
import dev.marlondvg.mc_deck_api.domain.Deck;
import dev.marlondvg.mc_deck_api.domain.exception.DeckNotFoundException;
import dev.marlondvg.mc_deck_api.domain.exception.InvalidDeckException;
import dev.marlondvg.mc_deck_api.infrastructure.config.CorsConfig;
import dev.marlondvg.mc_deck_api.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DeckController.class)
@Import({SecurityConfig.class, CorsConfig.class})
class DeckControllerTest {

	private static final UUID OWNER_ID = UUID.randomUUID();
	private static final UUID DECK_ID = UUID.randomUUID();
	private static final Instant CREATED = Instant.parse("2026-01-01T10:00:00Z");
	private static final String DECK_JSON = """
			{"name": "Spider-Man", "heroCode": "01001a", "aspects": ["justice"],
			 "slots": {"01002": 1, "01060": 3}}
			""";
	private static final DeckContents CONTENTS =
			new DeckContents("Spider-Man", "01001a", List.of("justice"), Map.of("01002", 1, "01060", 3));

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CreateDeckUseCase createDeck;
	@MockitoBean
	private GetDeckUseCase getDeck;
	@MockitoBean
	private ListDecksUseCase listDecks;
	@MockitoBean
	private UpdateDeckUseCase updateDeck;
	@MockitoBean
	private DeleteDeckUseCase deleteDeck;

	private static RequestPostProcessor owner() {
		return jwt().jwt(token -> token.subject(OWNER_ID.toString()));
	}

	private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
		return request.with(owner()).contentType(MediaType.APPLICATION_JSON).content(body);
	}

	private static Deck deck() {
		return Deck.restore(DECK_ID, OWNER_ID, "Spider-Man", "01001a", List.of("justice"),
				Map.of("01002", 1), CREATED, CREATED);
	}

	@Test
	void shouldCreateDeckForCurrentUserAndReturnLocation() throws Exception {
		when(createDeck.createDeck(eq(OWNER_ID), any())).thenReturn(deck());

		mockMvc.perform(json(post("/api/decks"), DECK_JSON))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", endsWith("/api/decks/" + DECK_ID)))
				.andExpect(jsonPath("$.id").value(DECK_ID.toString()))
				.andExpect(jsonPath("$.heroCode").value("01001a"))
				.andExpect(jsonPath("$.aspects[0]").value("justice"))
				.andExpect(jsonPath("$.slots['01002']").value(1))
				.andExpect(jsonPath("$.ownerId").doesNotExist());

		verify(createDeck).createDeck(OWNER_ID, CONTENTS);
	}

	@Test
	void shouldListGetUpdateAndDeleteDecks() throws Exception {
		when(listDecks.listDecks(OWNER_ID)).thenReturn(List.of(deck()));
		when(getDeck.getDeck(OWNER_ID, DECK_ID)).thenReturn(deck());
		when(updateDeck.updateDeck(OWNER_ID, DECK_ID, CONTENTS)).thenReturn(deck());

		mockMvc.perform(get("/api/decks").with(owner()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)));
		mockMvc.perform(get("/api/decks/{id}", DECK_ID).with(owner()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Spider-Man"));
		mockMvc.perform(json(put("/api/decks/{id}", DECK_ID), DECK_JSON))
				.andExpect(status().isOk());
		mockMvc.perform(delete("/api/decks/{id}", DECK_ID).with(owner()))
				.andExpect(status().isNoContent());

		verify(deleteDeck).deleteDeck(OWNER_ID, DECK_ID);
	}

	@Test
	void shouldAnswer404ForMissingDeck() throws Exception {
		when(getDeck.getDeck(OWNER_ID, DECK_ID)).thenThrow(new DeckNotFoundException(DECK_ID));
		doThrow(new DeckNotFoundException(DECK_ID)).when(deleteDeck).deleteDeck(OWNER_ID, DECK_ID);

		mockMvc.perform(get("/api/decks/{id}", DECK_ID).with(owner()))
				.andExpect(status().isNotFound())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
		mockMvc.perform(delete("/api/decks/{id}", DECK_ID).with(owner()))
				.andExpect(status().isNotFound());
	}

	@Test
	void shouldRejectMissingFieldsBeforeCallingUseCase() throws Exception {
		mockMvc.perform(json(post("/api/decks"), "{\"name\": \"\", \"aspects\": []}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[?(@.field == 'name')]").exists())
				.andExpect(jsonPath("$.errors[?(@.field == 'heroCode')]").exists())
				.andExpect(jsonPath("$.errors[?(@.field == 'aspects')]").exists())
				.andExpect(jsonPath("$.errors[?(@.field == 'slots')]").exists());

		verifyNoInteractions(createDeck);
	}

	@Test
	void shouldAnswer400ForDomainValidationErrors() throws Exception {
		when(createDeck.createDeck(eq(OWNER_ID), any())).thenThrow(new InvalidDeckException("unknown aspect: pink"));

		mockMvc.perform(json(post("/api/decks"), DECK_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("unknown aspect: pink"));
	}

	@Test
	void shouldRequireToken() throws Exception {
		mockMvc.perform(get("/api/decks")).andExpect(status().isUnauthorized());
		verifyNoInteractions(listDecks);
	}
}

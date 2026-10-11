package dev.marlondvg.mc_deck_api.infrastructure.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the whole stack (controller, service, JPA, H2 with Flyway) to check that
 * one user can never reach another user's decks or collection.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DeckOwnershipIntegrationTest {

	private static final String DECK_JSON = """
			{"name": "Spider-Man", "heroCode": "01001a", "aspects": ["justice"], "slots": {"01002": 1}}
			""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private UUID userA;
	private UUID userB;
	private String deckOfB;

	@BeforeEach
	void setUp() throws Exception {
		userA = insertUser("user_a");
		userB = insertUser("user_b");
		String location = mockMvc.perform(json(post("/api/decks"), userB, DECK_JSON))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getHeader("Location");
		deckOfB = location.substring(location.lastIndexOf('/') + 1);
	}

	private UUID insertUser(String username) {
		UUID id = UUID.randomUUID();
		jdbcTemplate.update("INSERT INTO users (id, username, password_hash, created_at) VALUES (?, ?, ?, ?)",
				id, username, "not-a-real-hash", Timestamp.from(Instant.now()));
		return id;
	}

	private static RequestPostProcessor as(UUID user) {
		return jwt().jwt(token -> token.subject(user.toString()));
	}

	private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, UUID user, String body) {
		return request.with(as(user)).contentType(MediaType.APPLICATION_JSON).content(body);
	}

	@Test
	void shouldNotLetUserReadAnotherUsersDeck() throws Exception {
		mockMvc.perform(get("/api/decks/{id}", deckOfB).with(as(userA))).andExpect(status().isNotFound());
	}

	@Test
	void shouldNotLetUserUpdateAnotherUsersDeck() throws Exception {
		mockMvc.perform(json(put("/api/decks/{id}", deckOfB), userA, DECK_JSON)).andExpect(status().isNotFound());
		mockMvc.perform(get("/api/decks/{id}", deckOfB).with(as(userB)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Spider-Man"));
	}

	@Test
	void shouldNotLetUserDeleteAnotherUsersDeck() throws Exception {
		mockMvc.perform(delete("/api/decks/{id}", deckOfB).with(as(userA))).andExpect(status().isNotFound());
		mockMvc.perform(get("/api/decks/{id}", deckOfB).with(as(userB))).andExpect(status().isOk());
	}

	@Test
	void shouldListOnlyOwnDecks() throws Exception {
		mockMvc.perform(get("/api/decks").with(as(userA))).andExpect(jsonPath("$", hasSize(0)));
		mockMvc.perform(get("/api/decks").with(as(userB))).andExpect(jsonPath("$", hasSize(1)));
	}

	@Test
	void shouldKeepCollectionsSeparate() throws Exception {
		mockMvc.perform(json(put("/api/collection"), userB, "{\"packs\": [\"core\"]}")).andExpect(status().isOk());

		mockMvc.perform(get("/api/collection").with(as(userA))).andExpect(jsonPath("$.packs", hasSize(0)));
		mockMvc.perform(get("/api/collection").with(as(userB))).andExpect(jsonPath("$.packs[0]").value("core"));
	}

	@Test
	void shouldRunFullDeckLifecycleForOwner() throws Exception {
		mockMvc.perform(json(put("/api/decks/{id}", deckOfB), userB, """
						{"name": "Renamed", "heroCode": "01001a", "aspects": ["pool"], "slots": {"01061": 2}}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Renamed"))
				.andExpect(jsonPath("$.slots['01061']").value(2))
				.andExpect(jsonPath("$.slots['01002']").doesNotExist());
		mockMvc.perform(delete("/api/decks/{id}", deckOfB).with(as(userB))).andExpect(status().isNoContent());
		mockMvc.perform(get("/api/decks/{id}", deckOfB).with(as(userB))).andExpect(status().isNotFound());
	}
}

package dev.marlondvg.mc_deck_api.infrastructure.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Real tokens end to end: register, log in, then call a protected endpoint with the issued JWT.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthFlowIntegrationTest {

	private static final String PASSWORD = "correct horse battery";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
		return request.contentType(MediaType.APPLICATION_JSON).content(body);
	}

	private void register(String username) throws Exception {
		mockMvc.perform(json(post("/api/auth/register"), """
						{"username": "%s", "password": "%s"}
						""".formatted(username, PASSWORD)))
				.andExpect(status().isCreated());
	}

	private String login(String username) throws Exception {
		String body = mockMvc.perform(json(post("/api/auth/login"), """
						{"username": "%s", "password": "%s"}
						""".formatted(username, PASSWORD)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.accessToken");
	}

	@Test
	void shouldReturnCurrentUserForIssuedToken() throws Exception {
		register("Erin");
		// Login accepts the username in any letter case.
		String token = login("ERIN");

		mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("erin"))
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void shouldRejectTokenOfUserThatDoesNotExist() throws Exception {
		mockMvc.perform(get("/api/users/me").with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail").value("User no longer exists"));
	}

	@Test
	void shouldStoreBcryptHashAndNormalizedUsername() throws Exception {
		register("Bob");

		String hash = jdbcTemplate.queryForObject(
				"SELECT password_hash FROM users WHERE username = ?", String.class, "bob");
		assertThat(hash).startsWith("$2").isNotEqualTo(PASSWORD);
	}

	@Test
	void shouldRejectSecondRegistrationWithSameUsernameInAnyCase() throws Exception {
		register("carol");

		mockMvc.perform(json(post("/api/auth/register"), """
						{"username": "CAROL", "password": "%s"}
						""".formatted(PASSWORD)))
				.andExpect(status().isConflict());
	}

	@Test
	void shouldGiveSameAnswerForWrongPasswordAndUnknownUsername() throws Exception {
		register("dan");

		String wrongPassword = mockMvc.perform(json(post("/api/auth/login"), """
						{"username": "dan", "password": "not the password"}
						"""))
				.andExpect(status().isUnauthorized())
				.andReturn().getResponse().getContentAsString();
		String unknownUsername = mockMvc.perform(json(post("/api/auth/login"), """
						{"username": "nobody", "password": "not the password"}
						"""))
				.andExpect(status().isUnauthorized())
				.andReturn().getResponse().getContentAsString();

		assertThat(JsonPath.<String>read(wrongPassword, "$.detail"))
				.isEqualTo(JsonPath.<String>read(unknownUsername, "$.detail"))
				.isEqualTo("Invalid username or password");
	}

	@Test
	void shouldBlockUsernameAfterFiveFailedLoginsEvenWithCorrectPassword() throws Exception {
		register("frank");
		for (int attempt = 1; attempt <= 5; attempt++) {
			mockMvc.perform(json(post("/api/auth/login"), """
							{"username": "frank", "password": "not the password"}
							""")
							.with(request -> {
								request.setRemoteAddr("192.0.2.10");
								return request;
							}))
					.andExpect(status().isUnauthorized());
		}

		mockMvc.perform(json(post("/api/auth/login"), """
						{"username": "frank", "password": "%s"}
						""".formatted(PASSWORD))
						.with(request -> {
							request.setRemoteAddr("192.0.2.11");
							return request;
						}))
				.andExpect(status().isTooManyRequests())
				.andExpect(header().exists(HttpHeaders.RETRY_AFTER))
				.andExpect(jsonPath("$.detail").value("Too many failed login attempts. Try again later."));
	}

	@Test
	void shouldRejectRequestWithoutToken() throws Exception {
		mockMvc.perform(get("/api/users/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").value("Authentication is required"));
	}

	@Test
	void shouldRejectInvalidToken() throws Exception {
		mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.valid.token"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer error=\"invalid_token\"")))
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").value("The access token is invalid or expired"));
	}

	@Test
	void shouldExposeHealthAndApiDocsWithoutToken() throws Exception {
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
	}

	@Test
	void shouldAllowCorsPreflightFromFrontendOrigin() throws Exception {
		mockMvc.perform(options("/api/users/me")
						.header(HttpHeaders.ORIGIN, "http://localhost:5173")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
	}

	@Test
	void shouldRefuseCorsPreflightFromUnknownOrigin() throws Exception {
		mockMvc.perform(options("/api/users/me")
						.header(HttpHeaders.ORIGIN, "https://evil.example.com")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}
}

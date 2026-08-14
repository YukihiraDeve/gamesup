package com.gamesup.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import com.gamesup.api.GamesUpApplication;
import com.gamesup.api.auth.domain.Role;
import com.gamesup.api.auth.domain.User;
import com.gamesup.api.auth.infrastructure.persistence.UserRepository;
import com.gamesup.api.testsupport.MySqlIntegrationTest;

@SpringBootTest(properties = {
		"spring.flyway.enabled=true",
		"spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(classes = { GamesUpApplication.class, AuthSecurityIntegrationTest.TestEndpoints.class })
class AuthSecurityIntegrationTest extends MySqlIntegrationTest {

	private static final String JWT_SECRET = "test-only-signing-key-with-at-least-32-bytes";
	private static final String RAW_PASSWORD = "a-secure-password";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void deleteUsers() {
		userRepository.deleteAll();
	}

	@Test
	void registersOnlyClientsAndHashesPassword() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "Alice.Dupont@Example.com",
						  "password": "a-secure-password",
						  "firstName": "Alice",
						  "lastName": "Dupont",
						  "role": "ADMIN"
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.email").value("alice.dupont@example.com"))
				.andExpect(jsonPath("$.role").value("CLIENT"));

		User saved = userRepository.findByEmailIgnoreCase("ALICE.DUPONT@EXAMPLE.COM").orElseThrow();
		assertThat(saved.getRole()).isEqualTo(Role.CLIENT);
		assertThat(saved.getPasswordHash()).isNotEqualTo(RAW_PASSWORD);
		assertThat(passwordEncoder.matches(RAW_PASSWORD, saved.getPasswordHash())).isTrue();
	}

	@Test
	void rejectsInvalidRegistrationFields() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "invalid-email",
						  "password": "short",
						  "firstName": "",
						  "lastName": "Dupont"
						}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.errors.email").exists())
				.andExpect(jsonPath("$.errors.password").exists());
	}

	@Test
	void rejectsDuplicateEmailIgnoringCase() throws Exception {
		register("client@example.com");

		mockMvc.perform(post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(registrationJson("CLIENT@EXAMPLE.COM")))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(409));
	}

	@Test
	void logsInWithValidCredentials() throws Exception {
		saveUser("client@example.com", Role.CLIENT);

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginJson("client@example.com", RAW_PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.role").value("CLIENT"));
	}

	@Test
	void rejectsInvalidCredentialsWithoutRevealingWhichFieldFailed() throws Exception {
		saveUser("client@example.com", Role.CLIENT);

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginJson("client@example.com", "incorrect-password")))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.detail").value("Invalid email or password."));
	}

	@Test
	void rejectsProtectedRouteWithoutToken() throws Exception {
		mockMvc.perform(get("/api/v1/test/protected"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void rejectsExpiredToken() throws Exception {
		saveUser("client@example.com", Role.CLIENT);

		mockMvc.perform(get("/api/v1/test/protected")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken("client@example.com", Role.CLIENT)))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.detail").value("Access token is invalid or expired."));
	}

	@Test
	void refusesAdminRouteToClientRole() throws Exception {
		saveUser("client@example.com", Role.CLIENT);
		String token = loginToken("client@example.com");

		mockMvc.perform(get("/api/v1/admin/test")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isForbidden())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(403));
	}

	@Test
	void allowsAdminRouteToAdminRole() throws Exception {
		saveUser("admin@example.com", Role.ADMIN);
		String token = loginToken("admin@example.com");

		mockMvc.perform(get("/api/v1/admin/test")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("admin"));
	}

	@Test
	void leavesCatalogReadingPublic() throws Exception {
		mockMvc.perform(get("/api/v1/games"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray())
				.andExpect(jsonPath("$.page").value(0));

		mockMvc.perform(get("/api/v1/games/999999"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void appliesCorsConfigurationFromProperties() throws Exception {
		mockMvc.perform(options("/api/v1/test/protected")
				.header(HttpHeaders.ORIGIN, "http://localhost:4200")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));
	}

	private void register(String email) throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(registrationJson(email)))
				.andExpect(status().isCreated());
	}

	private void saveUser(String email, Role role) {
		userRepository.saveAndFlush(new User(
				email,
				passwordEncoder.encode(RAW_PASSWORD),
				"Test",
				"User",
				role,
				true));
	}

	private String loginToken(String email) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginJson(email, RAW_PASSWORD)))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode response = objectMapper.readTree(result.getResponse().getContentAsByteArray());
		return response.get("accessToken").asText();
	}

	private static String registrationJson(String email) {
		return """
				{
				  "email": "%s",
				  "password": "a-secure-password",
				  "firstName": "Test",
				  "lastName": "User"
				}
				""".formatted(email);
	}

	private static String loginJson(String email, String password) {
		return """
				{
				  "email": "%s",
				  "password": "%s"
				}
				""".formatted(email, password);
	}

	private static String expiredToken(String email, Role role) {
		SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
		Instant now = Instant.now();
		return Jwts.builder()
				.issuer("gamesup-api")
				.subject(email)
				.claim("role", role.name())
				.issuedAt(Date.from(now.minusSeconds(7200)))
				.expiration(Date.from(now.minusSeconds(3600)))
				.signWith(key)
				.compact();
	}

	@RestController
	static class TestEndpoints {

		@GetMapping("/api/v1/test/protected")
		Map<String, String> protectedRoute() {
			return Map.of("message", "authenticated");
		}

		@GetMapping("/api/v1/admin/test")
		Map<String, String> adminRoute() {
			return Map.of("message", "admin");
		}
	}
}

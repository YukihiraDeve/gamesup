package com.gamesup.api.customer.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.gamesup.api.auth.domain.Role;
import com.gamesup.api.auth.domain.User;
import com.gamesup.api.auth.infrastructure.persistence.UserRepository;

@SpringBootTest(properties = {
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserProfileAdminUserControllerTest {

	private static final String RAW_PASSWORD = "a-secure-password";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void requiresAuthenticationForCurrentProfile() throws Exception {
		mockMvc.perform(get("/api/v1/users/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void derivesOwnershipFromTokenAndNeverSerializesPasswordData() throws Exception {
		User owner = saveUser("owner-profile@example.com", Role.CLIENT);
		User other = saveUser("other-profile@example.com", Role.CLIENT);
		String ownerToken = loginToken(owner.getEmail());

		mockMvc.perform(get("/api/v1/users/me")
				.header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(owner.getId()))
				.andExpect(jsonPath("$.email").value(owner.getEmail()))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist())
				.andExpect(jsonPath("$.version").doesNotExist());

		mockMvc.perform(patch("/api/v1/users/me")
				.header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "id": %d,
						  "firstName": "Updated Owner"
						}
						""".formatted(other.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(owner.getId()))
				.andExpect(jsonPath("$.firstName").value("Updated Owner"));

		assertThat(userRepository.findById(owner.getId()).orElseThrow().getFirstName())
				.isEqualTo("Updated Owner");
		assertThat(userRepository.findById(other.getId()).orElseThrow().getFirstName())
				.isEqualTo("Test");
	}

	@Test
	void validatesProfilePatchAndReportsDuplicateEmail() throws Exception {
		User owner = saveUser("owner-unique@example.com", Role.CLIENT);
		saveUser("already-used@example.com", Role.CLIENT);
		String token = loginToken(owner.getEmail());

		mockMvc.perform(patch("/api/v1/users/me")
				.header(HttpHeaders.AUTHORIZATION, bearer(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isBadRequest());

		mockMvc.perform(patch("/api/v1/users/me")
				.header(HttpHeaders.AUTHORIZATION, bearer(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"lastName\":\"   \"}"))
				.andExpect(status().isBadRequest());

		mockMvc.perform(patch("/api/v1/users/me")
				.header(HttpHeaders.AUTHORIZATION, bearer(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"ALREADY-USED@example.com\"}"))
				.andExpect(status().isConflict());
	}

	@Test
	void refusesAdminAccountsToClientRole() throws Exception {
		User client = saveUser("client-admin-users@example.com", Role.CLIENT);
		String token = loginToken(client.getEmail());

		mockMvc.perform(get("/api/v1/admin/users")
				.header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isForbidden());

		mockMvc.perform(patch("/api/v1/admin/users/{id}/role", client.getId())
				.header(HttpHeaders.AUTHORIZATION, bearer(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"role\":\"ADMIN\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void letsAdminListInspectDisableAndPromoteAnotherAccount() throws Exception {
		User administrator = saveUser("admin-users@example.com", Role.ADMIN);
		User client = saveUser("managed-client@example.com", Role.CLIENT);
		String adminToken = loginToken(administrator.getEmail());
		String clientToken = loginToken(client.getEmail());

		mockMvc.perform(get("/api/v1/admin/users")
				.header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
				.param("page", "0")
				.param("size", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.totalElements").value(2))
				.andExpect(jsonPath("$.totalPages").value(2))
				.andExpect(jsonPath("$.content[0].passwordHash").doesNotExist());

		mockMvc.perform(get("/api/v1/admin/users/{id}", client.getId())
				.header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(client.getEmail()))
				.andExpect(jsonPath("$.passwordHash").doesNotExist());

		mockMvc.perform(patch("/api/v1/admin/users/{id}/role", client.getId())
				.header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"role\":\"ADMIN\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("ADMIN"));

		mockMvc.perform(patch("/api/v1/admin/users/{id}/enabled", client.getId())
				.header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"enabled\":false}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.enabled").value(false));

		mockMvc.perform(get("/api/v1/users/me")
				.header(HttpHeaders.AUTHORIZATION, bearer(clientToken)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void preventsAdministratorFromDisablingOrDemotingSelf() throws Exception {
		User administrator = saveUser("protected-admin@example.com", Role.ADMIN);
		String token = loginToken(administrator.getEmail());

		mockMvc.perform(patch("/api/v1/admin/users/{id}/enabled", administrator.getId())
				.header(HttpHeaders.AUTHORIZATION, bearer(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"enabled\":false}"))
				.andExpect(status().isForbidden());

		mockMvc.perform(patch("/api/v1/admin/users/{id}/role", administrator.getId())
				.header(HttpHeaders.AUTHORIZATION, bearer(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"role\":\"CLIENT\"}"))
				.andExpect(status().isForbidden());
	}

	private User saveUser(String email, Role role) {
		return userRepository.saveAndFlush(new User(
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
				.content("""
						{
						  "email": "%s",
						  "password": "%s"
						}
						""".formatted(email, RAW_PASSWORD)))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode response = objectMapper.readTree(result.getResponse().getContentAsByteArray());
		return response.get("accessToken").asText();
	}

	private static String bearer(String token) {
		return "Bearer " + token;
	}
}

package com.gamesup.api.customer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.gamesup.api.auth.domain.Role;
import com.gamesup.api.auth.domain.User;
import com.gamesup.api.auth.infrastructure.persistence.UserRepository;
import com.gamesup.api.common.application.exception.ConflictException;
import com.gamesup.api.common.application.exception.ResourceNotFoundException;
import com.gamesup.api.customer.web.dto.UserProfileUpdateRequest;

@DataJpaTest(properties = {
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
@Import({UserProfileService.class, UserMapper.class})
class UserProfileServiceTest {

	@Autowired
	private UserProfileService userProfileService;

	@Autowired
	private UserRepository userRepository;

	@Test
	void readsAndPartiallyUpdatesOnlyCurrentProfile() {
		User user = saveUser("owner@example.com", "Original", "Name", Role.CLIENT);

		var before = userProfileService.findCurrentUser(user.getId());
		var updated = userProfileService.updateCurrentUser(
				user.getId(),
				new UserProfileUpdateRequest(null, "Updated", null));

		assertThat(before.email()).isEqualTo("owner@example.com");
		assertThat(updated.id()).isEqualTo(user.getId());
		assertThat(updated.firstName()).isEqualTo("Updated");
		assertThat(updated.lastName()).isEqualTo("Name");
		assertThat(updated.role()).isEqualTo(Role.CLIENT);
		assertThat(updated.enabled()).isTrue();
	}

	@Test
	void normalizesEmailAndRefusesEmailOwnedByAnotherAccount() {
		User owner = saveUser("owner@example.com", "Owner", "User", Role.CLIENT);
		saveUser("used@example.com", "Other", "User", Role.CLIENT);

		var updated = userProfileService.updateCurrentUser(
				owner.getId(),
				new UserProfileUpdateRequest("  NEW@Example.COM ", null, null));

		assertThat(updated.email()).isEqualTo("new@example.com");
		assertThatThrownBy(() -> userProfileService.updateCurrentUser(
				owner.getId(),
				new UserProfileUpdateRequest("USED@example.com", null, null)))
				.isInstanceOf(ConflictException.class);
	}

	@Test
	void rejectsAnUnknownCurrentUser() {
		assertThatThrownBy(() -> userProfileService.findCurrentUser(Long.MAX_VALUE))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	private User saveUser(String email, String firstName, String lastName, Role role) {
		return userRepository.saveAndFlush(new User(
				email,
				"$2a$10$test-only-password-hash",
				firstName,
				lastName,
				role,
				true));
	}
}

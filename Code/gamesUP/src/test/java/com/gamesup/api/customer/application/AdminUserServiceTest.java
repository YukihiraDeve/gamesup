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
import com.gamesup.api.common.application.exception.ForbiddenOperationException;

@DataJpaTest(properties = {
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
@Import({AdminUserService.class, UserMapper.class})
class AdminUserServiceTest {

	@Autowired
	private AdminUserService adminUserService;

	@Autowired
	private UserRepository userRepository;

	@Test
	void returnsPagedAccountsAndChangesTargetState() {
		User administrator = saveUser("admin@example.com", Role.ADMIN);
		User client = saveUser("client@example.com", Role.CLIENT);

		var page = adminUserService.findAll(0, 1);
		var disabled = adminUserService.changeEnabled(administrator.getId(), client.getId(), false);
		var promoted = adminUserService.changeRole(administrator.getId(), client.getId(), Role.ADMIN);

		assertThat(page.content()).hasSize(1);
		assertThat(page.totalElements()).isEqualTo(2);
		assertThat(page.totalPages()).isEqualTo(2);
		assertThat(adminUserService.findById(client.getId()).email()).isEqualTo("client@example.com");
		assertThat(disabled.enabled()).isFalse();
		assertThat(promoted.role()).isEqualTo(Role.ADMIN);
	}

	@Test
	void refusesSelfDisablingAndSelfDemotion() {
		User administrator = saveUser("self-admin@example.com", Role.ADMIN);

		assertThatThrownBy(() -> adminUserService.changeEnabled(
				administrator.getId(), administrator.getId(), false))
				.isInstanceOf(ForbiddenOperationException.class);
		assertThatThrownBy(() -> adminUserService.changeRole(
				administrator.getId(), administrator.getId(), Role.CLIENT))
				.isInstanceOf(ForbiddenOperationException.class);
	}

	private User saveUser(String email, Role role) {
		return userRepository.saveAndFlush(new User(
				email,
				"$2a$10$test-only-password-hash",
				"Test",
				"User",
				role,
				true));
	}
}

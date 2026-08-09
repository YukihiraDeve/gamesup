package com.gamesup.api.auth.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.gamesup.api.auth.domain.Role;
import com.gamesup.api.auth.domain.User;

@DataJpaTest(properties = {
		"spring.flyway.enabled=true",
		"spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers
class UserRepositoryTest {

	private static final String PASSWORD_HASH =
			"$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

	@Container
	@ServiceConnection
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.5");

	@Autowired
	private UserRepository userRepository;

	@Test
	void savesAuditFieldsAndFindsNormalizedEmailIgnoringCase() {
		User saved = userRepository.saveAndFlush(user(" Alice.Dupont@Example.com ", "Alice"));

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getEmail()).isEqualTo("alice.dupont@example.com");
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getUpdatedAt()).isNotNull();
		assertThat(saved.getVersion()).isZero();
		assertThat(userRepository.findByEmailIgnoreCase("ALICE.DUPONT@EXAMPLE.COM"))
				.contains(saved);
		assertThat(userRepository.existsByEmailIgnoreCase("Alice.Dupont@Example.com"))
				.isTrue();
	}

	@Test
	void rejectsDuplicateEmailRegardlessOfCase() {
		userRepository.saveAndFlush(user("client@example.com", "Premier"));

		assertThatThrownBy(() -> userRepository.saveAndFlush(user("CLIENT@EXAMPLE.COM", "Second")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	private static User user(String email, String firstName) {
		return new User(email, PASSWORD_HASH, firstName, "Dupont", Role.CLIENT, true);
	}
}

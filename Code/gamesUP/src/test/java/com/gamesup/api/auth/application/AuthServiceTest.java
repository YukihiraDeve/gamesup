package com.gamesup.api.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.gamesup.api.auth.domain.Role;
import com.gamesup.api.auth.domain.User;
import com.gamesup.api.auth.infrastructure.persistence.UserRepository;
import com.gamesup.api.auth.infrastructure.security.JwtService;
import com.gamesup.api.auth.web.dto.LoginRequest;
import com.gamesup.api.auth.web.dto.RegisterRequest;
import com.gamesup.api.config.infrastructure.properties.JwtProperties;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private AuthenticationManager authenticationManager;

	@Mock
	private JwtService jwtService;

	@Mock
	private Authentication authentication;

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(
				userRepository,
				passwordEncoder,
				authenticationManager,
				jwtService,
				new JwtProperties(
						"gamesup-test",
						Duration.ofMinutes(15),
						"test-only-signing-key-with-at-least-32-bytes"));
	}

	@Test
	void registersAClientWithEncodedPasswordAndGeneratedToken() {
		when(userRepository.existsByEmailIgnoreCase("Alice@Example.com")).thenReturn(false);
		when(passwordEncoder.encode("a-secure-password")).thenReturn("encoded-password");
		when(jwtService.generateToken(any())).thenReturn("signed-token");

		var response = authService.register(new RegisterRequest(
				"Alice@Example.com",
				"a-secure-password",
				"Alice",
				"User"));

		ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
		verify(userRepository).saveAndFlush(savedUser.capture());
		assertThat(savedUser.getValue().getEmail()).isEqualTo("alice@example.com");
		assertThat(savedUser.getValue().getPasswordHash()).isEqualTo("encoded-password");
		assertThat(savedUser.getValue().getRole()).isEqualTo(Role.CLIENT);
		assertThat(response.accessToken()).isEqualTo("signed-token");
		assertThat(response.expiresInSeconds()).isEqualTo(900);
	}

	@Test
	void rejectsDuplicatesDetectedBeforeOrDuringPersistence() {
		RegisterRequest request = new RegisterRequest(
				"duplicate@example.com",
				"a-secure-password",
				"Duplicate",
				"User");
		when(userRepository.existsByEmailIgnoreCase(request.email())).thenReturn(true);

		assertThatThrownBy(() -> authService.register(request))
				.isInstanceOf(DuplicateEmailException.class);
		verify(passwordEncoder, never()).encode(any());

		when(userRepository.existsByEmailIgnoreCase(request.email())).thenReturn(false);
		when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
		when(userRepository.saveAndFlush(any(User.class)))
				.thenThrow(new DataIntegrityViolationException("unique constraint"));

		assertThatThrownBy(() -> authService.register(request))
				.isInstanceOf(DuplicateEmailException.class);
	}

	@Test
	void authenticatesThenBuildsLoginResponseFromPersistedUser() {
		User user = new User(
				"client@example.com",
				"encoded-password",
				"Client",
				"User",
				Role.CLIENT,
				true);
		GamesUpUserPrincipal principal = GamesUpUserPrincipal.from(user);
		when(authenticationManager.authenticate(any())).thenReturn(authentication);
		when(authentication.getPrincipal()).thenReturn(principal);
		when(userRepository.findById(principal.userId())).thenReturn(Optional.of(user));
		when(jwtService.generateToken(principal)).thenReturn("login-token");

		var response = authService.login(new LoginRequest("client@example.com", "a-secure-password"));

		assertThat(response.accessToken()).isEqualTo("login-token");
		assertThat(response.email()).isEqualTo("client@example.com");
		assertThat(response.role()).isEqualTo(Role.CLIENT);
		verify(authenticationManager).authenticate(any());
	}
}

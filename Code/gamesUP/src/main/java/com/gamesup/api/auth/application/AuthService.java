package com.gamesup.api.auth.application;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gamesup.api.auth.domain.Role;
import com.gamesup.api.auth.domain.User;
import com.gamesup.api.auth.infrastructure.persistence.UserRepository;
import com.gamesup.api.auth.infrastructure.security.JwtService;
import com.gamesup.api.auth.web.dto.AuthenticationResponse;
import com.gamesup.api.auth.web.dto.LoginRequest;
import com.gamesup.api.auth.web.dto.RegisterRequest;
import com.gamesup.api.config.infrastructure.properties.JwtProperties;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
	private final JwtProperties jwtProperties;

	public AuthService(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			AuthenticationManager authenticationManager,
			JwtService jwtService,
			JwtProperties jwtProperties) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
		this.jwtProperties = jwtProperties;
	}

	@Transactional
	public AuthenticationResponse register(RegisterRequest request) {
		if (userRepository.existsByEmailIgnoreCase(request.email())) {
			throw new DuplicateEmailException();
		}

		User user = new User(
				request.email(),
				passwordEncoder.encode(request.password()),
				request.firstName(),
				request.lastName(),
				Role.CLIENT,
				true);

		try {
			userRepository.saveAndFlush(user);
		} catch (DataIntegrityViolationException exception) {
			throw new DuplicateEmailException();
		}

		UserDetails principal = GamesUpUserPrincipal.from(user);
		return response(user, jwtService.generateToken(principal));
	}

	@Transactional(readOnly = true)
	public AuthenticationResponse login(LoginRequest request) {
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.email(), request.password()));
		GamesUpUserPrincipal principal = (GamesUpUserPrincipal) authentication.getPrincipal();
		User user = userRepository.findById(principal.userId())
				.orElseThrow();
		return response(user, jwtService.generateToken(principal));
	}

	private AuthenticationResponse response(User user, String token) {
		return new AuthenticationResponse(
				token,
				"Bearer",
				jwtProperties.accessTokenTtl().toSeconds(),
				user.getId(),
				user.getEmail(),
				user.getRole());
	}
}

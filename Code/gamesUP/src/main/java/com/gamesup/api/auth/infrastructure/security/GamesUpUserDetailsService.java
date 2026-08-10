package com.gamesup.api.auth.infrastructure.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.gamesup.api.auth.application.GamesUpUserPrincipal;
import com.gamesup.api.auth.infrastructure.persistence.UserRepository;

@Service
public class GamesUpUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public GamesUpUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		return userRepository.findByEmailIgnoreCase(email)
				.map(GamesUpUserPrincipal::from)
				.orElseThrow(() -> new UsernameNotFoundException("Invalid credentials."));
	}
}

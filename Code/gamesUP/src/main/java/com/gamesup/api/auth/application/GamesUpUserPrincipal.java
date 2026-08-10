package com.gamesup.api.auth.application;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.gamesup.api.auth.domain.Role;
import com.gamesup.api.auth.domain.User;

public record GamesUpUserPrincipal(
		Long userId,
		String username,
		String password,
		Role role,
		boolean enabled) implements UserDetails {

	public static GamesUpUserPrincipal from(User user) {
		return new GamesUpUserPrincipal(
				user.getId(),
				user.getEmail(),
				user.getPasswordHash(),
				user.getRole(),
				user.isEnabled());
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}
}

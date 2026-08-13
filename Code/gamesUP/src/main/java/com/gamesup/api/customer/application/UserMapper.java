package com.gamesup.api.customer.application;

import org.springframework.stereotype.Component;

import com.gamesup.api.auth.domain.User;
import com.gamesup.api.customer.web.dto.UserResponse;

@Component
public class UserMapper {

	public UserResponse toResponse(User user) {
		return new UserResponse(
				user.getId(),
				user.getEmail(),
				user.getFirstName(),
				user.getLastName(),
				user.getRole(),
				user.isEnabled(),
				user.getCreatedAt(),
				user.getUpdatedAt());
	}
}

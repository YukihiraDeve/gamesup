package com.gamesup.api.customer.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gamesup.api.auth.domain.User;
import com.gamesup.api.auth.infrastructure.persistence.UserRepository;
import com.gamesup.api.common.application.exception.ConflictException;
import com.gamesup.api.common.application.exception.ResourceNotFoundException;
import com.gamesup.api.customer.web.dto.UserProfileUpdateRequest;
import com.gamesup.api.customer.web.dto.UserResponse;

@Service
public class UserProfileService {

	private static final Logger LOGGER = LoggerFactory.getLogger(UserProfileService.class);

	private final UserRepository userRepository;
	private final UserMapper userMapper;

	public UserProfileService(UserRepository userRepository, UserMapper userMapper) {
		this.userRepository = userRepository;
		this.userMapper = userMapper;
	}

	@Transactional(readOnly = true)
	public UserResponse findCurrentUser(Long userId) {
		return userMapper.toResponse(findUser(userId));
	}

	@Transactional
	public UserResponse updateCurrentUser(Long userId, UserProfileUpdateRequest request) {
		User user = findUser(userId);
		String email = request.email() == null ? user.getEmail() : request.email();
		String firstName = request.firstName() == null ? user.getFirstName() : request.firstName();
		String lastName = request.lastName() == null ? user.getLastName() : request.lastName();
		if (userRepository.existsByEmailIgnoreCaseAndIdNot(email.trim(), userId)) {
			throw duplicateEmail();
		}

		user.updateProfile(email, firstName, lastName);
		try {
			userRepository.flush();
		} catch (DataIntegrityViolationException exception) {
			throw duplicateEmail();
		}
		LOGGER.info("User profile updated: userId={}", userId);
		return userMapper.toResponse(user);
	}

	private User findUser(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User " + userId + " was not found."));
	}

	private static ConflictException duplicateEmail() {
		return new ConflictException("An account already exists for this email address.");
	}
}

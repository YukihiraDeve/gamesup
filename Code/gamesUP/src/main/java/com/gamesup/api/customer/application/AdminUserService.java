package com.gamesup.api.customer.application;

import static com.gamesup.api.common.web.validation.ValidationRules.PAGE_SIZE_MAX;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gamesup.api.auth.domain.Role;
import com.gamesup.api.auth.domain.User;
import com.gamesup.api.auth.infrastructure.persistence.UserRepository;
import com.gamesup.api.common.application.exception.ForbiddenOperationException;
import com.gamesup.api.common.application.exception.InvalidRequestException;
import com.gamesup.api.common.application.exception.ResourceNotFoundException;
import com.gamesup.api.common.web.dto.PageResponse;
import com.gamesup.api.customer.web.dto.UserResponse;

@Service
public class AdminUserService {

	private static final Logger LOGGER = LoggerFactory.getLogger(AdminUserService.class);

	private final UserRepository userRepository;
	private final UserMapper userMapper;

	public AdminUserService(UserRepository userRepository, UserMapper userMapper) {
		this.userRepository = userRepository;
		this.userMapper = userMapper;
	}

	@Transactional(readOnly = true)
	public PageResponse<UserResponse> findAll(int page, int size) {
		if (page < 0 || size < 1 || size > PAGE_SIZE_MAX) {
			throw new InvalidRequestException(
					"Page must be non-negative and size must be between 1 and " + PAGE_SIZE_MAX + ".");
		}
		return PageResponse.from(
				userRepository.findAll(PageRequest.of(page, size, Sort.by("id").ascending())),
				userMapper::toResponse);
	}

	@Transactional(readOnly = true)
	public UserResponse findById(Long userId) {
		return userMapper.toResponse(findUser(userId));
	}

	@Transactional
	public UserResponse changeEnabled(Long administratorId, Long userId, boolean enabled) {
		if (administratorId.equals(userId) && !enabled) {
			throw new ForbiddenOperationException("Administrators cannot disable their own account.");
		}
		User user = findUser(userId);
		user.setEnabled(enabled);
		userRepository.flush();
		LOGGER.info("User activation changed: administratorId={}, userId={}, enabled={}",
				administratorId, userId, enabled);
		return userMapper.toResponse(user);
	}

	@Transactional
	public UserResponse changeRole(Long administratorId, Long userId, Role role) {
		if (administratorId.equals(userId) && role != Role.ADMIN) {
			throw new ForbiddenOperationException("Administrators cannot demote their own account.");
		}
		User user = findUser(userId);
		user.changeRole(role);
		userRepository.flush();
		LOGGER.info("User role changed: administratorId={}, userId={}, role={}",
				administratorId, userId, role);
		return userMapper.toResponse(user);
	}

	private User findUser(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User " + userId + " was not found."));
	}
}

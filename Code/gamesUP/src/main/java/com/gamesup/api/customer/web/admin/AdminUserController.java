package com.gamesup.api.customer.web.admin;

import static com.gamesup.api.common.web.validation.ValidationRules.PAGE_MIN;
import static com.gamesup.api.common.web.validation.ValidationRules.PAGE_SIZE_MAX;
import static com.gamesup.api.common.web.validation.ValidationRules.PAGE_SIZE_MIN;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gamesup.api.auth.application.GamesUpUserPrincipal;
import com.gamesup.api.common.web.dto.PageResponse;
import com.gamesup.api.customer.application.AdminUserService;
import com.gamesup.api.customer.web.admin.dto.AdminUserEnabledRequest;
import com.gamesup.api.customer.web.admin.dto.AdminUserRoleRequest;
import com.gamesup.api.customer.web.dto.UserResponse;

@Validated
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

	private final AdminUserService adminUserService;

	public AdminUserController(AdminUserService adminUserService) {
		this.adminUserService = adminUserService;
	}

	@GetMapping
	public PageResponse<UserResponse> findAll(
			@RequestParam(defaultValue = "0") @Min(PAGE_MIN) int page,
			@RequestParam(defaultValue = "20") @Min(PAGE_SIZE_MIN) @Max(PAGE_SIZE_MAX) int size) {
		return adminUserService.findAll(page, size);
	}

	@GetMapping("/{userId}")
	public UserResponse findById(@PathVariable @Positive Long userId) {
		return adminUserService.findById(userId);
	}

	@PatchMapping("/{userId}/enabled")
	public UserResponse changeEnabled(
			@AuthenticationPrincipal GamesUpUserPrincipal principal,
			@PathVariable @Positive Long userId,
			@Valid @RequestBody AdminUserEnabledRequest request) {
		return adminUserService.changeEnabled(principal.userId(), userId, request.enabled());
	}

	@PatchMapping("/{userId}/role")
	public UserResponse changeRole(
			@AuthenticationPrincipal GamesUpUserPrincipal principal,
			@PathVariable @Positive Long userId,
			@Valid @RequestBody AdminUserRoleRequest request) {
		return adminUserService.changeRole(principal.userId(), userId, request.role());
	}
}

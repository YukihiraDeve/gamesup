package com.gamesup.api.customer.web.admin.dto;

import jakarta.validation.constraints.NotNull;

import com.gamesup.api.auth.domain.Role;

public record AdminUserRoleRequest(@NotNull Role role) {
}

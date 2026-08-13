package com.gamesup.api.customer.web.admin.dto;

import jakarta.validation.constraints.NotNull;

public record AdminUserEnabledRequest(@NotNull Boolean enabled) {
}

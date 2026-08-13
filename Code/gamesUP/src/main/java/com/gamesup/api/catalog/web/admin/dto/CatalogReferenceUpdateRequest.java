package com.gamesup.api.catalog.web.admin.dto;

import static com.gamesup.api.common.web.validation.ValidationRules.CATALOG_NAME_MAX_LENGTH;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CatalogReferenceUpdateRequest(
		@NotBlank @Size(max = CATALOG_NAME_MAX_LENGTH) String name) {
}

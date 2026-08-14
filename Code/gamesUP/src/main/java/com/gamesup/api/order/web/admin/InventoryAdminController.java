package com.gamesup.api.order.web.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gamesup.api.order.application.InventoryAdminService;
import com.gamesup.api.order.web.admin.dto.InventoryResponse;
import com.gamesup.api.order.web.admin.dto.InventoryUpdateRequest;
import com.gamesup.api.config.web.OpenApiConfiguration;

@Validated
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/v1/admin/inventory")
@Tag(name = "Inventory administration", description = "Stock absolu d'un jeu, réservé aux ADMIN.")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
public class InventoryAdminController {

	private final InventoryAdminService inventoryAdminService;

	public InventoryAdminController(InventoryAdminService inventoryAdminService) {
		this.inventoryAdminService = inventoryAdminService;
	}

	@GetMapping("/{gameId}")
	@Operation(summary = "Consulter le stock d'un jeu")
	public InventoryResponse find(@PathVariable @Positive Long gameId) {
		return inventoryAdminService.findByGameId(gameId);
	}

	@PutMapping("/{gameId}")
	@Operation(summary = "Définir le stock absolu")
	public InventoryResponse setAbsoluteQuantity(
			@PathVariable @Positive Long gameId,
			@Valid @RequestBody InventoryUpdateRequest request) {
		return inventoryAdminService.setAbsoluteQuantity(gameId, request.quantity());
	}
}

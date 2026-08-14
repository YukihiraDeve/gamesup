package com.gamesup.api.order.web.admin;

import static com.gamesup.api.common.web.validation.ValidationRules.PAGE_MIN;
import static com.gamesup.api.common.web.validation.ValidationRules.PAGE_SIZE_MAX;
import static com.gamesup.api.common.web.validation.ValidationRules.PAGE_SIZE_MIN;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gamesup.api.common.web.dto.PageResponse;
import com.gamesup.api.config.web.OpenApiConfiguration;
import com.gamesup.api.order.application.AdminOrderService;
import com.gamesup.api.order.web.admin.dto.AdminOrderStatusRequest;
import com.gamesup.api.order.web.dto.OrderResponse;

@Validated
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/v1/admin/orders")
@Tag(name = "Order administration", description = "Consultation et machine d'état réservées aux ADMIN.")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
public class AdminOrderController {

	private final AdminOrderService adminOrderService;

	public AdminOrderController(AdminOrderService adminOrderService) {
		this.adminOrderService = adminOrderService;
	}

	@GetMapping
	@Operation(summary = "Lister toutes les commandes")
	public PageResponse<OrderResponse> findAll(
			@RequestParam(defaultValue = "0") @Min(PAGE_MIN) int page,
			@RequestParam(defaultValue = "20") @Min(PAGE_SIZE_MIN) @Max(PAGE_SIZE_MAX) int size) {
		return adminOrderService.findAll(page, size);
	}

	@PatchMapping("/{orderId}/status")
	@Operation(
			summary = "Faire évoluer le statut",
			description = "Seules les transitions définies par la machine d'état sont acceptées.")
	public OrderResponse transition(
			@PathVariable @Positive Long orderId,
			@Valid @RequestBody AdminOrderStatusRequest request) {
		return adminOrderService.transition(orderId, request.status());
	}
}

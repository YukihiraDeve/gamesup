package com.gamesup.api.order.web;

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

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.gamesup.api.auth.application.GamesUpUserPrincipal;
import com.gamesup.api.common.web.dto.PageResponse;
import com.gamesup.api.config.web.OpenApiConfiguration;
import com.gamesup.api.order.application.OrderService;
import com.gamesup.api.order.web.dto.OrderCreateRequest;
import com.gamesup.api.order.web.dto.OrderResponse;

@Validated
@RestController
@PreAuthorize("hasRole('CLIENT')")
@RequestMapping("/api/v1/orders")
@Tag(
		name = "Orders",
		description = "Commandes du CLIENT connecté ; prix recalculés côté serveur et stock verrouillé.")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
public class OrderController {

	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(
			summary = "Créer une commande",
			description = "Le corps accepte uniquement les identifiants de jeux et quantités ; tout prix reçu est ignoré.")
	public OrderResponse create(
			@AuthenticationPrincipal GamesUpUserPrincipal principal,
			@Valid @RequestBody OrderCreateRequest request) {
		return orderService.create(principal.userId(), request.lines());
	}

	@GetMapping
	@Operation(summary = "Lister ses commandes")
	public PageResponse<OrderResponse> findCurrentUserOrders(
			@AuthenticationPrincipal GamesUpUserPrincipal principal,
			@RequestParam(defaultValue = "0") @Min(PAGE_MIN) int page,
			@RequestParam(defaultValue = "20") @Min(PAGE_SIZE_MIN) @Max(PAGE_SIZE_MAX) int size) {
		return orderService.findCurrentUserOrders(principal.userId(), page, size);
	}

	@GetMapping("/{orderId}")
	@Operation(summary = "Consulter une de ses commandes")
	public OrderResponse findCurrentUserOrder(
			@AuthenticationPrincipal GamesUpUserPrincipal principal,
			@PathVariable @Positive Long orderId) {
		return orderService.findCurrentUserOrder(principal.userId(), orderId);
	}
}

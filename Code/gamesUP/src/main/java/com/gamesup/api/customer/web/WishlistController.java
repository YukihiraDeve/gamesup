package com.gamesup.api.customer.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.gamesup.api.auth.application.GamesUpUserPrincipal;
import com.gamesup.api.config.web.OpenApiConfiguration;
import com.gamesup.api.customer.application.WishlistService;
import com.gamesup.api.customer.web.dto.WishlistResponse;

@RestController
@PreAuthorize("hasRole('CLIENT')")
@RequestMapping("/api/v1/users/me/wishlist")
@Tag(
		name = "Wishlist",
		description = "Liste de souhaits privée du CLIENT identifié par le JWT.")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
public class WishlistController {

	private final WishlistService wishlistService;

	public WishlistController(WishlistService wishlistService) {
		this.wishlistService = wishlistService;
	}

	@GetMapping
	@Operation(summary = "Consulter sa wishlist")
	public WishlistResponse findCurrentWishlist(
			@AuthenticationPrincipal GamesUpUserPrincipal principal) {
		return wishlistService.findCurrentWishlist(principal.userId());
	}

	@PutMapping("/{gameId}")
	@Operation(
			summary = "Ajouter un jeu",
			description = "Opération idempotente : un ajout répété ne crée pas de doublon.")
	public WishlistResponse addGame(
			@AuthenticationPrincipal GamesUpUserPrincipal principal,
			@PathVariable Long gameId) {
		return wishlistService.addGame(principal.userId(), gameId);
	}

	@DeleteMapping("/{gameId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(
			summary = "Retirer un jeu",
			description = "La suppression d'un jeu absent reste idempotente.")
	public void removeGame(
			@AuthenticationPrincipal GamesUpUserPrincipal principal,
			@PathVariable Long gameId) {
		wishlistService.removeGame(principal.userId(), gameId);
	}
}

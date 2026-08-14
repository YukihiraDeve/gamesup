package com.gamesup.api.customer.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gamesup.api.auth.domain.User;
import com.gamesup.api.auth.infrastructure.persistence.UserRepository;
import com.gamesup.api.catalog.application.GameResponseMapper;
import com.gamesup.api.catalog.domain.Game;
import com.gamesup.api.catalog.infrastructure.persistence.GameRepository;
import com.gamesup.api.catalog.web.dto.GameSummaryResponse;
import com.gamesup.api.common.application.exception.ResourceNotFoundException;
import com.gamesup.api.customer.domain.Wishlist;
import com.gamesup.api.customer.domain.WishlistItem;
import com.gamesup.api.customer.infrastructure.persistence.WishlistItemRepository;
import com.gamesup.api.customer.infrastructure.persistence.WishlistRepository;
import com.gamesup.api.customer.web.dto.WishlistResponse;

@Service
public class WishlistService {

	private final UserRepository userRepository;
	private final GameRepository gameRepository;
	private final WishlistRepository wishlistRepository;
	private final WishlistItemRepository wishlistItemRepository;
	private final GameResponseMapper gameResponseMapper;

	public WishlistService(
			UserRepository userRepository,
			GameRepository gameRepository,
			WishlistRepository wishlistRepository,
			WishlistItemRepository wishlistItemRepository,
			GameResponseMapper gameResponseMapper) {
		this.userRepository = userRepository;
		this.gameRepository = gameRepository;
		this.wishlistRepository = wishlistRepository;
		this.wishlistItemRepository = wishlistItemRepository;
		this.gameResponseMapper = gameResponseMapper;
	}

	@Transactional(readOnly = true)
	public WishlistResponse findCurrentWishlist(Long userId) {
		return wishlistRepository.findByUserId(userId)
				.map(this::toResponse)
				.orElseGet(() -> new WishlistResponse(List.of()));
	}

	@Transactional
	public WishlistResponse addGame(Long userId, Long gameId) {
		Game game = gameRepository.findByIdAndActiveTrue(gameId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Active game " + gameId + " was not found."));
		Wishlist wishlist = wishlistRepository.findByUserId(userId)
				.orElseGet(() -> createWishlist(userId));

		if (!wishlistItemRepository.existsByWishlistIdAndGameId(wishlist.getId(), gameId)) {
			wishlistItemRepository.saveAndFlush(new WishlistItem(wishlist, game));
		}
		return toResponse(wishlist);
	}

	@Transactional
	public void removeGame(Long userId, Long gameId) {
		wishlistRepository.findByUserId(userId)
				.ifPresent(wishlist -> wishlistItemRepository.deleteByWishlistIdAndGameId(
						wishlist.getId(), gameId));
	}

	private Wishlist createWishlist(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User " + userId + " was not found."));
		return wishlistRepository.saveAndFlush(new Wishlist(user));
	}

	private WishlistResponse toResponse(Wishlist wishlist) {
		List<GameSummaryResponse> games = wishlistItemRepository
				.findAllByWishlistIdOrderByAddedAtAscIdAsc(wishlist.getId())
				.stream()
				.map(WishlistItem::getGame)
				.map(gameResponseMapper::toSummary)
				.toList();
		return new WishlistResponse(games);
	}
}

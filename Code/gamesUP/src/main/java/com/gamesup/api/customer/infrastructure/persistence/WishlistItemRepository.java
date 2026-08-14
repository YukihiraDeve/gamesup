package com.gamesup.api.customer.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.gamesup.api.customer.domain.WishlistItem;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

	@EntityGraph(attributePaths = {"game", "game.publisher", "game.authors", "game.categories"})
	List<WishlistItem> findAllByWishlistIdOrderByAddedAtAscIdAsc(Long wishlistId);

	boolean existsByWishlistIdAndGameId(Long wishlistId, Long gameId);

	long deleteByWishlistIdAndGameId(Long wishlistId, Long gameId);
}

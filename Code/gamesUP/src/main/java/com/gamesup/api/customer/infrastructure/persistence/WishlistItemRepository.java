package com.gamesup.api.customer.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gamesup.api.customer.domain.WishlistItem;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

	List<WishlistItem> findAllByWishlistId(Long wishlistId);

	boolean existsByWishlistIdAndGameId(Long wishlistId, Long gameId);
}

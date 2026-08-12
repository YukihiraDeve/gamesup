package com.gamesup.api.customer.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gamesup.api.customer.domain.Wishlist;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

	Optional<Wishlist> findByUserId(Long userId);
}

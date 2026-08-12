package com.gamesup.api.customer.domain;

import java.time.Instant;

import com.gamesup.api.catalog.domain.Game;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(
		name = "wishlist_items",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_wishlist_items_wishlist_game",
				columnNames = {"wishlist_id", "game_id"}))
public class WishlistItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "wishlist_id", nullable = false)
	private Wishlist wishlist;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "game_id", nullable = false)
	private Game game;

	@Column(name = "added_at", nullable = false, updatable = false)
	private Instant addedAt;

	protected WishlistItem() {
	}

	public WishlistItem(Wishlist wishlist, Game game) {
		this.wishlist = wishlist;
		this.game = game;
	}

	@PrePersist
	void onCreate() {
		addedAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public Wishlist getWishlist() {
		return wishlist;
	}

	public Game getGame() {
		return game;
	}

	public Instant getAddedAt() {
		return addedAt;
	}
}

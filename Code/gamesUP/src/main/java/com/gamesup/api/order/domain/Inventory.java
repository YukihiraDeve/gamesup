package com.gamesup.api.order.domain;

import com.gamesup.api.catalog.domain.Game;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(
		name = "inventories",
		uniqueConstraints = @UniqueConstraint(name = "uk_inventories_game", columnNames = "game_id"))
public class Inventory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "game_id", nullable = false)
	private Game game;

	@Min(0)
	@Column(nullable = false)
	private int quantity;

	@Version
	@Column(nullable = false)
	private long version;

	protected Inventory() {
	}

	public Inventory(Game game, int quantity) {
		this.game = game;
		this.quantity = quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public void decreaseBy(int orderedQuantity) {
		if (orderedQuantity < 1 || orderedQuantity > quantity) {
			throw new IllegalArgumentException("Ordered quantity must be available and positive.");
		}
		quantity -= orderedQuantity;
	}

	public Long getId() {
		return id;
	}

	public Game getGame() {
		return game;
	}

	public int getQuantity() {
		return quantity;
	}

	public long getVersion() {
		return version;
	}
}

package com.gamesup.api.order.domain;

import java.math.BigDecimal;

import com.gamesup.api.catalog.domain.Game;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(
		name = "order_lines",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_order_lines_order_game",
				columnNames = {"order_id", "game_id"}))
public class OrderLine {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	private Order order;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "game_id", nullable = false)
	private Game game;

	@Min(1)
	@Column(nullable = false)
	private int quantity;

	@NotNull
	@DecimalMin("0.00")
	@Digits(integer = 8, fraction = 2)
	@Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
	private BigDecimal unitPrice;

	protected OrderLine() {
	}

	public OrderLine(Order order, Game game, int quantity, BigDecimal unitPrice) {
		this.order = order;
		this.game = game;
		this.quantity = quantity;
		this.unitPrice = unitPrice;
	}

	public Long getId() {
		return id;
	}

	public Order getOrder() {
		return order;
	}

	public Game getGame() {
		return game;
	}

	public int getQuantity() {
		return quantity;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}
}

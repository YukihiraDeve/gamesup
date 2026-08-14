package com.gamesup.api.customer.domain;

import java.time.Instant;

import com.gamesup.api.auth.domain.User;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(
		name = "reviews",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_reviews_user_game",
				columnNames = {"user_id", "game_id"}))
public class Review {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "game_id", nullable = false)
	private Game game;

	@Min(1)
	@Max(5)
	@Column(nullable = false)
	private int rating;

	@Size(max = 2000)
	@Column(length = 2000)
	private String comment;

	@Column(nullable = false)
	private boolean hidden;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Review() {
	}

	public Review(User user, Game game, int rating, String comment) {
		this.user = user;
		this.game = game;
		this.rating = rating;
		this.comment = comment;
	}

	public void update(int rating, String comment) {
		this.rating = rating;
		this.comment = comment;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public Game getGame() {
		return game;
	}

	public int getRating() {
		return rating;
	}

	public String getComment() {
		return comment;
	}

	public boolean isHidden() {
		return hidden;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}

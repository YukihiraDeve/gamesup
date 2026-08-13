package com.gamesup.api.catalog.domain;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "games")
public class Game {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Size(max = 200)
	@Column(nullable = false, length = 200)
	private String name;

	@NotBlank
	@Size(max = 4000)
	@Column(nullable = false, columnDefinition = "TEXT")
	private String description;

	@NotNull
	@DecimalMin("0.00")
	@Digits(integer = 8, fraction = 2)
	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal price;

	@Min(1)
	@Column(name = "min_players", nullable = false)
	private int minPlayers;

	@Min(1)
	@Column(name = "max_players", nullable = false)
	private int maxPlayers;

	@Min(0)
	@Column(name = "min_age", nullable = false)
	private int minAge;

	@Min(1)
	@Column(name = "duration_minutes", nullable = false)
	private int durationMinutes;

	@Min(1)
	@Column(name = "edition_number", nullable = false)
	private int editionNumber;

	@Column(nullable = false)
	private boolean active;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "publisher_id", nullable = false)
	private Publisher publisher;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "game_authors",
			joinColumns = @JoinColumn(name = "game_id"),
			inverseJoinColumns = @JoinColumn(name = "author_id"))
	private Set<Author> authors = new LinkedHashSet<>();

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "game_categories",
			joinColumns = @JoinColumn(name = "game_id"),
			inverseJoinColumns = @JoinColumn(name = "category_id"))
	private Set<Category> categories = new LinkedHashSet<>();

	protected Game() {
	}

	public Game(
			String name,
			String description,
			BigDecimal price,
			int minPlayers,
			int maxPlayers,
			int minAge,
			int durationMinutes,
			int editionNumber,
			boolean active,
			Publisher publisher,
			Set<Author> authors,
			Set<Category> categories) {
		this.name = CatalogNameNormalizer.displayName(name);
		this.description = description;
		this.price = price;
		this.minPlayers = minPlayers;
		this.maxPlayers = maxPlayers;
		this.minAge = minAge;
		this.durationMinutes = durationMinutes;
		this.editionNumber = editionNumber;
		this.active = active;
		this.publisher = publisher;
		this.authors.addAll(authors);
		this.categories.addAll(categories);
	}

	@PrePersist
	@PreUpdate
	void normalizeName() {
		name = CatalogNameNormalizer.displayName(name);
	}

	public void update(
			String name,
			String description,
			BigDecimal price,
			int minPlayers,
			int maxPlayers,
			int minAge,
			int durationMinutes,
			int editionNumber,
			Publisher publisher,
			Set<Author> authors,
			Set<Category> categories) {
		this.name = CatalogNameNormalizer.displayName(name);
		this.description = description;
		this.price = price;
		this.minPlayers = minPlayers;
		this.maxPlayers = maxPlayers;
		this.minAge = minAge;
		this.durationMinutes = durationMinutes;
		this.editionNumber = editionNumber;
		this.publisher = publisher;
		this.authors.clear();
		this.authors.addAll(authors);
		this.categories.clear();
		this.categories.addAll(categories);
	}

	public void archive() {
		active = false;
	}

	@AssertTrue(message = "maxPlayers must be greater than or equal to minPlayers")
	public boolean isPlayerRangeValid() {
		return maxPlayers >= minPlayers;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public int getMinPlayers() {
		return minPlayers;
	}

	public int getMaxPlayers() {
		return maxPlayers;
	}

	public int getMinAge() {
		return minAge;
	}

	public int getDurationMinutes() {
		return durationMinutes;
	}

	public int getEditionNumber() {
		return editionNumber;
	}

	public boolean isActive() {
		return active;
	}

	public Publisher getPublisher() {
		return publisher;
	}

	public Set<Author> getAuthors() {
		return Collections.unmodifiableSet(authors);
	}

	public Set<Category> getCategories() {
		return Collections.unmodifiableSet(categories);
	}
}

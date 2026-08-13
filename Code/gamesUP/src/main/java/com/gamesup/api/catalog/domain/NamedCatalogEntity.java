package com.gamesup.api.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@MappedSuperclass
public abstract class NamedCatalogEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Size(max = 150)
	@Column(nullable = false, length = 150)
	private String name;

	@NotBlank
	@Size(max = 150)
	@Column(name = "normalized_name", nullable = false, length = 150)
	private String normalizedName;

	protected NamedCatalogEntity() {
	}

	protected NamedCatalogEntity(String name) {
		setNames(name);
	}

	@PrePersist
	@PreUpdate
	void normalizeName() {
		setNames(name);
	}

	private void setNames(String value) {
		name = CatalogNameNormalizer.displayName(value);
		normalizedName = CatalogNameNormalizer.normalizedName(value);
	}

	public void rename(String newName) {
		setNames(newName);
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getNormalizedName() {
		return normalizedName;
	}
}

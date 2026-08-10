package com.gamesup.api.catalog.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "categories",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_categories_normalized_name",
				columnNames = "normalized_name"))
public class Category extends NamedCatalogEntity {

	protected Category() {
	}

	public Category(String name) {
		super(name);
	}
}

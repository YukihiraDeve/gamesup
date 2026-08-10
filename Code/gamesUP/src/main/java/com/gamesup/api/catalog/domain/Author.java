package com.gamesup.api.catalog.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "authors",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_authors_normalized_name",
				columnNames = "normalized_name"))
public class Author extends NamedCatalogEntity {

	protected Author() {
	}

	public Author(String name) {
		super(name);
	}
}

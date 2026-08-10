package com.gamesup.api.catalog.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "publishers",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_publishers_normalized_name",
				columnNames = "normalized_name"))
public class Publisher extends NamedCatalogEntity {

	protected Publisher() {
	}

	public Publisher(String name) {
		super(name);
	}
}

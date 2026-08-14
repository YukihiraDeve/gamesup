package com.gamesup.api.testsupport;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;

public final class DatabaseCleaner {

	private static final List<String> TABLES = List.of(
			"order_lines",
			"orders",
			"reviews",
			"wishlist_items",
			"wishlists",
			"inventories",
			"game_authors",
			"game_categories",
			"games",
			"authors",
			"categories",
			"publishers",
			"users");

	private final JdbcTemplate jdbcTemplate;

	public DatabaseCleaner(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void clean() {
		jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
		try {
			TABLES.forEach(table -> jdbcTemplate.execute("TRUNCATE TABLE " + table));
		} finally {
			jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
		}
	}
}

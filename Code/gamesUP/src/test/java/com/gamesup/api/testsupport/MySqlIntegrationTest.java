package com.gamesup.api.testsupport;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

public abstract class MySqlIntegrationTest {

	private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.5")
			.withDatabaseName("gamesup_test")
			.withUsername("gamesup_test")
			.withPassword("gamesup_test");

	static {
		MYSQL.start();
	}

	@DynamicPropertySource
	protected static void mysqlProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
		registry.add("spring.datasource.username", MYSQL::getUsername);
		registry.add("spring.datasource.password", MYSQL::getPassword);
		registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
		registry.add("spring.flyway.enabled", () -> true);
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
	}
}

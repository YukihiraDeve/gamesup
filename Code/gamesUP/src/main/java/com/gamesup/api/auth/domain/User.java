package com.gamesup.api.auth.domain;

import java.time.Instant;
import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(
		name = "users",
		uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Email
	@Size(max = 320)
	@Column(nullable = false, length = 320)
	private String email;

	@NotBlank
	@Size(max = 255)
	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@NotBlank
	@Size(max = 100)
	@Column(name = "first_name", nullable = false, length = 100)
	private String firstName;

	@NotBlank
	@Size(max = 100)
	@Column(name = "last_name", nullable = false, length = 100)
	private String lastName;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role;

	@Column(nullable = false)
	private boolean enabled;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(nullable = false)
	private long version;

	protected User() {
	}

	public User(
			String email,
			String passwordHash,
			String firstName,
			String lastName,
			Role role,
			boolean enabled) {
		this.email = normalizeEmail(email);
		this.passwordHash = passwordHash;
		this.firstName = firstName;
		this.lastName = lastName;
		this.role = role;
		this.enabled = enabled;
	}

	@PrePersist
	void onCreate() {
		email = normalizeEmail(email);
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		email = normalizeEmail(email);
		updatedAt = Instant.now();
	}

	private static String normalizeEmail(String value) {
		return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
	}

	public void updateProfile(String email, String firstName, String lastName) {
		this.email = normalizeEmail(email);
		this.firstName = firstName;
		this.lastName = lastName;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void changeRole(Role role) {
		this.role = role;
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public Role getRole() {
		return role;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public long getVersion() {
		return version;
	}
}

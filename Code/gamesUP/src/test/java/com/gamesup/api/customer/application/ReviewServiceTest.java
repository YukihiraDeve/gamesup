package com.gamesup.api.customer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.gamesup.api.auth.domain.Role;
import com.gamesup.api.auth.domain.User;
import com.gamesup.api.auth.infrastructure.persistence.UserRepository;
import com.gamesup.api.catalog.domain.Game;
import com.gamesup.api.catalog.domain.Publisher;
import com.gamesup.api.catalog.infrastructure.persistence.GameRepository;
import com.gamesup.api.catalog.infrastructure.persistence.PublisherRepository;
import com.gamesup.api.common.application.exception.ConflictException;
import com.gamesup.api.common.application.exception.ForbiddenOperationException;
import com.gamesup.api.common.application.exception.InvalidRequestException;
import com.gamesup.api.common.application.exception.ResourceNotFoundException;
import com.gamesup.api.customer.infrastructure.persistence.ReviewRepository;

@DataJpaTest(properties = {
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
@Import({ReviewService.class, ReviewMapper.class})
class ReviewServiceTest {

	@Autowired
	private ReviewService reviewService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PublisherRepository publisherRepository;

	@Autowired
	private GameRepository gameRepository;

	@Autowired
	private ReviewRepository reviewRepository;

	@Test
	void enforcesOneReviewPerUserAndOwnerOnlyMutations() {
		User owner = saveUser("review-owner@example.com", "Owner");
		User other = saveUser("review-other@example.com", "Other");
		Game game = saveGame("Reviewed game");

		var created = reviewService.create(owner.getId(), game.getId(), 4, "Initial review");

		assertThatThrownBy(() -> reviewService.create(owner.getId(), game.getId(), 5, "Duplicate"))
				.isInstanceOf(ConflictException.class);
		assertThatThrownBy(() -> reviewService.update(other.getId(), created.id(), 1, "Hijacked"))
				.isInstanceOf(ForbiddenOperationException.class);
		assertThatThrownBy(() -> reviewService.delete(other.getId(), created.id()))
				.isInstanceOf(ForbiddenOperationException.class);

		var updated = reviewService.update(owner.getId(), created.id(), 5, "Updated review");
		assertThat(updated.rating()).isEqualTo(5);
		assertThat(updated.comment()).isEqualTo("Updated review");

		reviewService.delete(owner.getId(), created.id());
		assertThat(reviewRepository.findById(created.id())).isEmpty();
	}

	@Test
	void hidesModeratedReviewsWithoutDeletingThemOrPublishingEmail() {
		User owner = saveUser("private-reviewer@example.com", "PublicName");
		Game game = saveGame("Moderated game");
		var created = reviewService.create(owner.getId(), game.getId(), 3, "Visible");

		var visiblePage = reviewService.findPublishedByGame(game.getId(), 0, 20);
		assertThat(visiblePage.content()).hasSize(1);
		assertThat(visiblePage.content().getFirst().reviewerName()).isEqualTo("PublicName");

		reviewService.changeVisibility(created.id(), true);

		assertThat(reviewService.findPublishedByGame(game.getId(), 0, 20).content()).isEmpty();
		assertThat(reviewRepository.findById(created.id())).isPresent();
	}

	@Test
	void validatesRatingCommentAndReferencedResources() {
		User owner = saveUser("review-validation@example.com", "Reviewer");
		Game game = saveGame("Validation game");

		assertThatThrownBy(() -> reviewService.create(owner.getId(), game.getId(), 0, null))
				.isInstanceOf(InvalidRequestException.class);
		assertThatThrownBy(() -> reviewService.create(
				owner.getId(), game.getId(), 3, "x".repeat(2001)))
				.isInstanceOf(InvalidRequestException.class);
		assertThatThrownBy(() -> reviewService.create(owner.getId(), Long.MAX_VALUE, 3, null))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	private User saveUser(String email, String firstName) {
		return userRepository.saveAndFlush(new User(
				email,
				"$2a$10$test-only-password-hash",
				firstName,
				"Reviewer",
				Role.CLIENT,
				true));
	}

	private Game saveGame(String name) {
		Publisher publisher = publisherRepository.saveAndFlush(new Publisher(name + " publisher"));
		return gameRepository.saveAndFlush(new Game(
				name,
				"A game used by review service tests.",
				new BigDecimal("34.90"),
				2,
				4,
				10,
				45,
				1,
				true,
				publisher,
				Set.of(),
				Set.of()));
	}
}

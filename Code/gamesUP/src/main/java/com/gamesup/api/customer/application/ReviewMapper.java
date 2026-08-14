package com.gamesup.api.customer.application;

import org.springframework.stereotype.Component;

import com.gamesup.api.common.application.mapping.ResponseMapper;
import com.gamesup.api.customer.domain.Review;
import com.gamesup.api.customer.web.dto.ReviewResponse;

@Component
public class ReviewMapper implements ResponseMapper<Review, ReviewResponse> {

	@Override
	public ReviewResponse toResponse(Review review) {
		return new ReviewResponse(
				review.getId(),
				review.getGame().getId(),
				review.getRating(),
				review.getComment(),
				review.getUser().getFirstName(),
				review.getCreatedAt(),
				review.getUpdatedAt());
	}
}

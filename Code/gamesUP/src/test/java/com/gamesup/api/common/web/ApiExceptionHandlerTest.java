package com.gamesup.api.common.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gamesup.api.common.application.exception.BusinessRuleViolationException;
import com.gamesup.api.common.application.exception.ConflictException;
import com.gamesup.api.common.application.exception.ExternalServiceException;
import com.gamesup.api.common.application.exception.ForbiddenOperationException;
import com.gamesup.api.common.application.exception.InvalidExternalServiceResponseException;
import com.gamesup.api.common.application.exception.ResourceNotFoundException;

class ApiExceptionHandlerTest {

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
				.setControllerAdvice(new ApiExceptionHandler())
				.build();
	}

	@ParameterizedTest
	@MethodSource("businessProblems")
	void mapsBusinessErrorsToProblemDetails(
			String path,
			int expectedStatus,
			String expectedTitle,
			String expectedDetail) throws Exception {
		mockMvc.perform(get(path))
				.andExpect(status().is(expectedStatus))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type").value("urn:gamesup:problem:" + expectedStatus))
				.andExpect(jsonPath("$.title").value(expectedTitle))
				.andExpect(jsonPath("$.status").value(expectedStatus))
				.andExpect(jsonPath("$.detail").value(expectedDetail))
				.andExpect(jsonPath("$.instance").value(path));
	}

	@Test
	void mapsInvalidBodyFieldsToBadRequest() throws Exception {
		mockMvc.perform(post("/test/validation")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.errors.name").exists());
	}

	@Test
	void mapsMalformedJsonToBadRequest() throws Exception {
		mockMvc.perform(post("/test/validation")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\":"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail")
						.value("The request body is missing or contains invalid JSON."));
	}

	@Test
	void mapsIncorrectParameterToBadRequest() throws Exception {
		mockMvc.perform(get("/test/parameter").param("quantity", "many"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void mapsConstrainedParameterToBadRequest() throws Exception {
		mockMvc.perform(get("/test/constraint").param("quantity", "0"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.errors.quantity").exists());
	}

	@Test
	void hidesDatabaseDetailsBehindConflictProblem() throws Exception {
		mockMvc.perform(get("/test/database"))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail")
						.value("The operation conflicts with an existing resource or database constraint."));
	}

	private static Stream<Arguments> businessProblems() {
		return Stream.of(
				Arguments.of("/test/not-found", 404, "Resource not found", "Game not found."),
				Arguments.of("/test/conflict", 409, "Conflict", "Game already exists."),
				Arguments.of("/test/forbidden", 403, "Forbidden operation", "Operation denied."),
				Arguments.of("/test/business-rule", 422, "Business rule violation", "Insufficient stock."),
				Arguments.of(
						"/test/external-service",
						503,
						"External service unavailable",
						"Recommendation service unavailable."),
				Arguments.of(
						"/test/invalid-external-response",
						502,
						"Invalid external service response",
						"Recommendation response is invalid."));
	}

	@RestController
	@RequestMapping("/test")
	private static class TestController {

		@GetMapping("/not-found")
		void notFound() {
			throw new ResourceNotFoundException("Game not found.");
		}

		@GetMapping("/conflict")
		void conflict() {
			throw new ConflictException("Game already exists.");
		}

		@GetMapping("/forbidden")
		void forbidden() {
			throw new ForbiddenOperationException("Operation denied.");
		}

		@GetMapping("/business-rule")
		void businessRule() {
			throw new BusinessRuleViolationException("Insufficient stock.");
		}

		@GetMapping("/external-service")
		void externalService() {
			throw new ExternalServiceException("Recommendation service unavailable.");
		}

		@GetMapping("/invalid-external-response")
		void invalidExternalResponse() {
			throw new InvalidExternalServiceResponseException("Recommendation response is invalid.");
		}

		@GetMapping("/parameter")
		void parameter(@RequestParam int quantity) {
		}

		@GetMapping("/constraint")
		void constraint(@RequestParam @Positive int quantity) {
		}

		@PostMapping("/validation")
		void validation(@Valid @RequestBody TestRequest request) {
		}

		@GetMapping("/database")
		void database() {
			throw new DataIntegrityViolationException("Duplicate entry 'sensitive-value'");
		}
	}

	private record TestRequest(@NotBlank String name) {
	}
}

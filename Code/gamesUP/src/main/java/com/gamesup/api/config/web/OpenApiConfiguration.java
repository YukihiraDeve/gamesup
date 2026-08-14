package com.gamesup.api.config.web;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;

@Configuration
@OpenAPIDefinition(
		info = @Info(
				title = "GamesUP API",
				version = "1.0.0",
				description = """
						API REST du catalogue et des parcours clients GamesUP.
						Les ressources personnelles sont toujours déterminées par le jeton JWT :
						aucune route ne permet de sélectionner un autre utilisateur.
						""",
				contact = @Contact(name = "GamesUP"),
				license = @License(name = "Projet pédagogique")),
		servers = @Server(url = "/", description = "Serveur courant"))
@SecurityScheme(
		name = OpenApiConfiguration.BEARER_AUTH,
		type = SecuritySchemeType.HTTP,
		scheme = "bearer",
		bearerFormat = "JWT",
		description = "Jeton retourné par l'inscription ou la connexion.")
public class OpenApiConfiguration {

	public static final String BEARER_AUTH = "bearerAuth";

	@Bean
	OperationCustomizer standardErrorResponses() {
		return (operation, handlerMethod) -> {
			addProblemResponse(operation.getResponses(), "400", "Requête ou paramètre invalide.");

			boolean secured = AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), PreAuthorize.class)
					|| AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), PreAuthorize.class);
			if (secured) {
				addProblemResponse(operation.getResponses(), "401", "JWT absent, invalide ou expiré.");
				addProblemResponse(operation.getResponses(), "403", "Rôle ou propriété de la ressource insuffisant.");
			}

			String controller = handlerMethod.getBeanType().getSimpleName();
			if (!controller.equals("AuthController")) {
				addProblemResponse(operation.getResponses(), "404", "Ressource introuvable.");
			}
			if (controller.equals("AuthController")
					|| controller.equals("AdminCatalogController")
					|| controller.equals("UserProfileController")
					|| controller.equals("GameReviewController")) {
				addProblemResponse(operation.getResponses(), "409", "Conflit avec l'état courant.");
			}
			if (controller.equals("OrderController") || controller.equals("AdminOrderController")) {
				addProblemResponse(operation.getResponses(), "422", "Règle métier empêchant l'opération.");
			}
			return operation;
		};
	}

	private static void addProblemResponse(
			io.swagger.v3.oas.models.responses.ApiResponses responses,
			String status,
			String description) {
		responses.addApiResponse(
				status,
				new ApiResponse()
						.description(description)
						.content(new Content().addMediaType(
								MediaType.APPLICATION_PROBLEM_JSON_VALUE,
								new io.swagger.v3.oas.models.media.MediaType()
										.schema(new Schema<>().$ref("#/components/schemas/ProblemDetail")))));
	}
}

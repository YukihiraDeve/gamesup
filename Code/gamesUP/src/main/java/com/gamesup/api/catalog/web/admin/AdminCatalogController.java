package com.gamesup.api.catalog.web.admin;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.gamesup.api.catalog.application.AdminCatalogService;
import com.gamesup.api.catalog.web.admin.dto.AdminGameCreateRequest;
import com.gamesup.api.catalog.web.admin.dto.AdminGameResponse;
import com.gamesup.api.catalog.web.admin.dto.AdminGameUpdateRequest;
import com.gamesup.api.catalog.web.admin.dto.CatalogReferenceCreateRequest;
import com.gamesup.api.catalog.web.admin.dto.CatalogReferenceResponse;
import com.gamesup.api.catalog.web.admin.dto.CatalogReferenceUpdateRequest;
import com.gamesup.api.config.web.OpenApiConfiguration;

@Validated
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/v1/admin/catalog")
@Tag(
		name = "Catalog administration",
		description = "CRUD des jeux, auteurs, éditeurs et catégories réservé aux ADMIN.")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
public class AdminCatalogController {

	private final AdminCatalogService adminCatalogService;

	public AdminCatalogController(AdminCatalogService adminCatalogService) {
		this.adminCatalogService = adminCatalogService;
	}

	@PostMapping("/games")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Créer un jeu")
	public AdminGameResponse createGame(@Valid @RequestBody AdminGameCreateRequest request) {
		return adminCatalogService.createGame(request);
	}

	@GetMapping("/games/{id}")
	@Operation(summary = "Consulter un jeu, actif ou archivé")
	public AdminGameResponse findGame(@PathVariable @Positive Long id) {
		return adminCatalogService.findGame(id);
	}

	@PutMapping("/games/{id}")
	@Operation(summary = "Remplacer les données éditables d'un jeu")
	public AdminGameResponse updateGame(
			@PathVariable @Positive Long id,
			@Valid @RequestBody AdminGameUpdateRequest request) {
		return adminCatalogService.updateGame(id, request);
	}

	@DeleteMapping("/games/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Archiver un jeu", description = "L'archivage est logique et préserve l'historique.")
	public void archiveGame(@PathVariable @Positive Long id) {
		adminCatalogService.archiveGame(id);
	}

	@GetMapping("/authors")
	@Operation(summary = "Lister les auteurs")
	public List<CatalogReferenceResponse> findAuthors() {
		return adminCatalogService.findAuthors();
	}

	@PostMapping("/authors")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Créer un auteur")
	public CatalogReferenceResponse createAuthor(
			@Valid @RequestBody CatalogReferenceCreateRequest request) {
		return adminCatalogService.createAuthor(request.name());
	}

	@PutMapping("/authors/{id}")
	@Operation(summary = "Renommer un auteur")
	public CatalogReferenceResponse updateAuthor(
			@PathVariable @Positive Long id,
			@Valid @RequestBody CatalogReferenceUpdateRequest request) {
		return adminCatalogService.updateAuthor(id, request.name());
	}

	@DeleteMapping("/authors/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Supprimer un auteur inutilisé")
	public void deleteAuthor(@PathVariable @Positive Long id) {
		adminCatalogService.deleteAuthor(id);
	}

	@GetMapping("/publishers")
	@Operation(summary = "Lister les éditeurs")
	public List<CatalogReferenceResponse> findPublishers() {
		return adminCatalogService.findPublishers();
	}

	@PostMapping("/publishers")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Créer un éditeur")
	public CatalogReferenceResponse createPublisher(
			@Valid @RequestBody CatalogReferenceCreateRequest request) {
		return adminCatalogService.createPublisher(request.name());
	}

	@PutMapping("/publishers/{id}")
	@Operation(summary = "Renommer un éditeur")
	public CatalogReferenceResponse updatePublisher(
			@PathVariable @Positive Long id,
			@Valid @RequestBody CatalogReferenceUpdateRequest request) {
		return adminCatalogService.updatePublisher(id, request.name());
	}

	@DeleteMapping("/publishers/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Supprimer un éditeur inutilisé")
	public void deletePublisher(@PathVariable @Positive Long id) {
		adminCatalogService.deletePublisher(id);
	}

	@GetMapping("/categories")
	@Operation(summary = "Lister les catégories")
	public List<CatalogReferenceResponse> findCategories() {
		return adminCatalogService.findCategories();
	}

	@PostMapping("/categories")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Créer une catégorie")
	public CatalogReferenceResponse createCategory(
			@Valid @RequestBody CatalogReferenceCreateRequest request) {
		return adminCatalogService.createCategory(request.name());
	}

	@PutMapping("/categories/{id}")
	@Operation(summary = "Renommer une catégorie")
	public CatalogReferenceResponse updateCategory(
			@PathVariable @Positive Long id,
			@Valid @RequestBody CatalogReferenceUpdateRequest request) {
		return adminCatalogService.updateCategory(id, request.name());
	}

	@DeleteMapping("/categories/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Supprimer une catégorie inutilisée")
	public void deleteCategory(@PathVariable @Positive Long id) {
		adminCatalogService.deleteCategory(id);
	}
}

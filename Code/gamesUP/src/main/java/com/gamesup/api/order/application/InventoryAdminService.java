package com.gamesup.api.order.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gamesup.api.catalog.domain.Game;
import com.gamesup.api.catalog.infrastructure.persistence.GameRepository;
import com.gamesup.api.common.application.exception.InvalidRequestException;
import com.gamesup.api.common.application.exception.ResourceNotFoundException;
import com.gamesup.api.order.domain.Inventory;
import com.gamesup.api.order.infrastructure.persistence.InventoryRepository;
import com.gamesup.api.order.web.admin.dto.InventoryResponse;

@Service
public class InventoryAdminService {

	private static final Logger LOGGER = LoggerFactory.getLogger(InventoryAdminService.class);

	private final InventoryRepository inventoryRepository;
	private final GameRepository gameRepository;

	public InventoryAdminService(InventoryRepository inventoryRepository, GameRepository gameRepository) {
		this.inventoryRepository = inventoryRepository;
		this.gameRepository = gameRepository;
	}

	@Transactional(readOnly = true)
	public InventoryResponse findByGameId(Long gameId) {
		Inventory inventory = inventoryRepository.findByGameId(gameId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Inventory for game " + gameId + " was not found."));
		return toResponse(inventory);
	}

	@Transactional
	public InventoryResponse setAbsoluteQuantity(Long gameId, int quantity) {
		if (quantity < 0) {
			throw new InvalidRequestException("Inventory quantity must be greater than or equal to zero.");
		}
		Game game = gameRepository.findById(gameId)
				.orElseThrow(() -> new ResourceNotFoundException("Game " + gameId + " was not found."));
		Inventory inventory = inventoryRepository.findByGameId(gameId)
				.orElseGet(() -> new Inventory(game, quantity));
		inventory.setQuantity(quantity);
		Inventory saved = inventoryRepository.saveAndFlush(inventory);
		LOGGER.info("Inventory quantity set: gameId={}, quantity={}", gameId, quantity);
		return toResponse(saved);
	}

	private static InventoryResponse toResponse(Inventory inventory) {
		return new InventoryResponse(
				inventory.getId(),
				inventory.getGame().getId(),
				inventory.getQuantity(),
				inventory.getVersion());
	}
}

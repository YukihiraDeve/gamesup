package com.gamesup.api.order.infrastructure.persistence;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gamesup.api.order.domain.Inventory;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

	Optional<Inventory> findByGameId(Long gameId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select inventory from Inventory inventory where inventory.game.id = :gameId")
	Optional<Inventory> findByGameIdForUpdate(@Param("gameId") Long gameId);
}

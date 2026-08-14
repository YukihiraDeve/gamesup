package com.gamesup.api.order.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.gamesup.api.order.domain.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

	Page<Order> findAllByUserId(Long userId, Pageable pageable);

	Optional<Order> findByIdAndUserId(Long id, Long userId);
}

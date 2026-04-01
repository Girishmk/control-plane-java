package com.controlplane.order.service;

import com.controlplane.order.domain.Order;
import com.controlplane.order.domain.OrderStatus;
import com.controlplane.order.kafka.OrderEventProducer;
import com.controlplane.order.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Core order business logic.
 * - @Transactional: each method is a DB unit-of-work
 * - @Timed/@Counted: Micrometer instruments timing/call count
 * - @CircuitBreaker: Resilience4j protects downstream calls
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventProducer eventProducer;
    private final MeterRegistry meterRegistry;

    @Counted(value = "orders.created", description = "Total orders created")
    @Timed(value = "order.creation.latency", description = "Order creation latency")
    public Order createOrder(String userId, BigDecimal totalAmount) {
        log.info("Creating order for user={}", userId);

        Order order = Order.builder()
                .userId(userId)
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .build();

        Order saved = orderRepository.save(order);

        // Publish async event to Kafka
        eventProducer.publishOrderCreated(saved);

        meterRegistry.gauge("orders.active",
                orderRepository.countByStatus(OrderStatus.PENDING));

        log.info("Order created id={} user={}", saved.getId(), userId);
        return saved;
    }

    @Transactional(readOnly = true)
    public Order findById(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Order not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Order> findByUser(String userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable);
    }

    @Counted(value = "orders.confirmed")
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "confirmOrderFallback")
    @Retry(name = "inventoryService")
    public Order confirmOrder(UUID orderId) {
        Order order = findById(orderId);
        // In a real scenario: call inventory service here
        order.setStatus(OrderStatus.CONFIRMED);
        Order saved = orderRepository.save(order);
        eventProducer.publishOrderConfirmed(saved);
        return saved;
    }

    // Resilience4j fallback — invoked when circuit is open
    public Order confirmOrderFallback(UUID orderId, Exception ex) {
        log.warn("Inventory service unavailable, queuing order={} for retry", orderId);
        meterRegistry.counter("orders.confirmation.fallback").increment();
        Order order = findById(orderId);
        order.setStatus(OrderStatus.PENDING);
        return order;
    }

    public Order cancelOrder(UUID orderId, String userId) {
        Order order = findById(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new SecurityException("Cannot cancel order owned by another user");
        }
        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        eventProducer.publishOrderCancelled(saved);
        return saved;
    }
}

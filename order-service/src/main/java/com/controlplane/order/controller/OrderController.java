package com.controlplane.order.controller;

import com.controlplane.order.domain.Order;
import com.controlplane.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Order createOrder(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam BigDecimal amount) {
        return orderService.createOrder(userId, amount);
    }

    @GetMapping("/{id}")
    public Order getOrder(@PathVariable UUID id) {
        return orderService.findById(id);
    }

    @GetMapping
    public Page<Order> listOrders(
            @RequestHeader("X-User-Id") String userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return orderService.findByUser(userId, pageable);
    }

    @PutMapping("/{id}/confirm")
    public Order confirmOrder(@PathVariable UUID id) {
        return orderService.confirmOrder(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId) {
        orderService.cancelOrder(id, userId);
        return ResponseEntity.noContent().build();
    }
}

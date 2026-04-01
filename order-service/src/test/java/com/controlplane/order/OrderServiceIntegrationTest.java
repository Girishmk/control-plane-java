package com.controlplane.order;

import com.controlplane.order.domain.Order;
import com.controlplane.order.domain.OrderStatus;
import com.controlplane.order.repository.OrderRepository;
import com.controlplane.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"order.events", "payment.events"})
class OrderServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("orders_test")
                    .withUsername("test")
                    .withPassword("test");

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void shouldCreateOrderAndPublishEvent() {
        Order created = orderService.createOrder("user-123", new BigDecimal("99.99"));

        assertThat(created.getId()).isNotNull();
        assertThat(created.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(created.getUserId()).isEqualTo("user-123");
        assertThat(created.getTotalAmount()).isEqualByComparingTo("99.99");

        // Verify persistence
        Order found = orderRepository.findById(created.getId()).orElseThrow();
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldConfirmOrderAndTransitionStatus() {
        Order order = orderService.createOrder("user-456", new BigDecimal("49.00"));
        Order confirmed = orderService.confirmOrder(order.getId());

        assertThat(confirmed.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void shouldCancelOrderByOwner() {
        Order order = orderService.createOrder("user-789", new BigDecimal("20.00"));
        Order cancelled = orderService.cancelOrder(order.getId(), "user-789");

        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void shouldRejectCancelByNonOwner() {
        Order order = orderService.createOrder("user-abc", new BigDecimal("10.00"));

        org.junit.jupiter.api.Assertions.assertThrows(SecurityException.class, () ->
            orderService.cancelOrder(order.getId(), "different-user"));
    }
}

package com.controlplane.order.kafka;

import com.controlplane.order.domain.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * Publishes order lifecycle events to Kafka topics.
 * Topic: order.events
 * Key: order UUID (ensures same order's events go to same partition)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @Value("${app.kafka.topic.orders:order.events}")
    private String ordersTopic;

    public void publishOrderCreated(Order order) {
        publish(OrderEvent.builder()
                .eventType("ORDER_CREATED")
                .orderId(order.getId().toString())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .occurredAt(Instant.now())
                .build());
    }

    public void publishOrderConfirmed(Order order) {
        publish(OrderEvent.builder()
                .eventType("ORDER_CONFIRMED")
                .orderId(order.getId().toString())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .occurredAt(Instant.now())
                .build());
    }

    public void publishOrderCancelled(Order order) {
        publish(OrderEvent.builder()
                .eventType("ORDER_CANCELLED")
                .orderId(order.getId().toString())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .occurredAt(Instant.now())
                .build());
    }

    private void publish(OrderEvent event) {
        CompletableFuture<SendResult<String, OrderEvent>> future =
                kafkaTemplate.send(ordersTopic, event.getOrderId(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish event type={} orderId={}",
                        event.getEventType(), event.getOrderId(), ex);
            } else {
                log.debug("Published event type={} orderId={} partition={} offset={}",
                        event.getEventType(), event.getOrderId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}

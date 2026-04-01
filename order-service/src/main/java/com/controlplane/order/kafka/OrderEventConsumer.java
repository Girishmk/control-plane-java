package com.controlplane.order.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

/**
 * Consumes events from other services (e.g. payment confirmations).
 * Uses @RetryableTopic for automatic DLQ + exponential backoff retry.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    @RetryableTopic(
        attempts = "4",
        backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 10000),
        topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
        dltTopicSuffix = ".dlq"
    )
    @KafkaListener(topics = "payment.events", groupId = "order-service-group")
    public void onPaymentEvent(OrderEvent event) {
        log.info("Received payment event type={} orderId={}", event.getEventType(), event.getOrderId());

        switch (event.getEventType()) {
            case "PAYMENT_CONFIRMED" -> handlePaymentConfirmed(event);
            case "PAYMENT_FAILED"    -> handlePaymentFailed(event);
            default -> log.warn("Unknown payment event type={}", event.getEventType());
        }
    }

    @KafkaListener(topics = "order.events.dlq", groupId = "order-service-dlq")
    public void onDeadLetter(OrderEvent event) {
        log.error("Dead letter received orderId={} type={} — manual review required",
                event.getOrderId(), event.getEventType());
        // Alert / page on-call
    }

    private void handlePaymentConfirmed(OrderEvent event) {
        log.info("Payment confirmed for orderId={}", event.getOrderId());
        // Update order status to PROCESSING
    }

    private void handlePaymentFailed(OrderEvent event) {
        log.warn("Payment failed for orderId={}", event.getOrderId());
        // Update order status to CANCELLED
    }
}

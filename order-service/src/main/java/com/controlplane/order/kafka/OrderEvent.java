package com.controlplane.order.kafka;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class OrderEvent {
    private String eventType;   // ORDER_CREATED | ORDER_CONFIRMED | ORDER_CANCELLED
    private String orderId;
    private String userId;
    private BigDecimal totalAmount;
    private String status;
    private Instant occurredAt;
}

package com.controlplane.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Order Service — Spring Boot 3 microservice.
 *
 * Responsibilities:
 *   - CRUD for Order aggregates (PostgreSQL via JPA/Hibernate)
 *   - Publishes ORDER_* events to Kafka topic "order.events"
 *   - Consumes PAYMENT_* events from Kafka topic "payment.events"
 *   - Exposes /actuator/prometheus for Micrometer → Prometheus scraping
 *   - Registers with Eureka for service discovery
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaAuditing
@EnableKafka
@EnableRetry
@EnableScheduling
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}

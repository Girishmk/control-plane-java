package com.controlplane.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway — entry point for all external traffic.
 * Routes: /api/orders/** → order-service (lb://order-service)
 *         /api/users/**  → user-service  (lb://user-service)
 * Features: JWT auth, rate limiting (Redis), circuit breaker (Resilience4j)
 */
@SpringBootApplication
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}

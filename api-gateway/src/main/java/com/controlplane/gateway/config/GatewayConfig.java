package com.controlplane.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()

            .route("order-service", r -> r
                .path("/api/orders/**")
                .filters(f -> f
                    .requestRateLimiter(c -> c
                        .setRateLimiter(redisRateLimiter())
                        .setKeyResolver(exchange -> exchange.getPrincipal()
                            .map(p -> p.getName())
                            .defaultIfEmpty("anonymous")))
                    .circuitBreaker(c -> c
                        .setName("orderCB")
                        .setFallbackUri("forward:/fallback/orders"))
                    .retry(cfg -> cfg.setRetries(2)))
                .uri("lb://order-service"))

            .route("user-service", r -> r
                .path("/api/users/**")
                .filters(f -> f
                    .circuitBreaker(c -> c
                        .setName("userCB")
                        .setFallbackUri("forward:/fallback/users")))
                .uri("lb://user-service"))

            .build();
    }

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        // 10 requests/second, burst capacity 20
        return new RedisRateLimiter(10, 20, 1);
    }
}

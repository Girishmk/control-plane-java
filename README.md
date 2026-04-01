# Control Plane Java

A production-grade Java microservices platform demonstrating:
- **Kubernetes Control Plane** — Deployment, HPA, etcd, Scheduler
- **CI/CD Orchestration** — Helm charts + ArgoCD GitOps
- **Spring Cloud** — Config Server, Eureka, API Gateway, Resilience4j
- **Java Services** — Spring Boot 3, JPA/Hibernate, Kafka, Micrometer
- **Observability** — Prometheus, Grafana, Jaeger (OpenTelemetry)

---

## Project structure

```
control-plane-java/
├── pom.xml                         ← Maven parent BOM (Spring Cloud, Resilience4j, Testcontainers)
│
├── config-server/                  ← Spring Cloud Config Server (port 8888)
│   └── src/main/java/.../ConfigServerApplication.java
│
├── eureka-server/                  ← Service Discovery (port 8761)
│   └── src/main/java/.../EurekaServerApplication.java
│
├── api-gateway/                    ← Spring Cloud Gateway (port 8080)
│   └── src/main/java/.../gateway/
│       ├── ApiGatewayApplication.java
│       ├── config/GatewayConfig.java      ← Routes, rate limiting
│       └── filter/JwtAuthFilter.java      ← JWT validation, header propagation
│
├── order-service/                  ← Core microservice (port 8081)
│   ├── Dockerfile                  ← Multi-stage, layered JAR, non-root user
│   └── src/main/java/.../order/
│       ├── OrderServiceApplication.java
│       ├── controller/
│       │   ├── OrderController.java          ← REST endpoints
│       │   └── GlobalExceptionHandler.java   ← RFC 7807 Problem Details
│       ├── service/OrderService.java         ← @Transactional, @Timed, @CircuitBreaker
│       ├── domain/
│       │   ├── Order.java                    ← JPA entity, optimistic locking
│       │   ├── OrderItem.java
│       │   └── OrderStatus.java
│       ├── repository/OrderRepository.java   ← JPA + native queries
│       ├── kafka/
│       │   ├── OrderEvent.java
│       │   ├── OrderEventProducer.java       ← Async publish, idempotent producer
│       │   └── OrderEventConsumer.java       ← @RetryableTopic + DLQ
│       └── metrics/MetricsConfig.java        ← Micrometer, @Timed, @Counted aspects
│
├── helm/order-service/             ← Helm chart for Kubernetes deployment
│   ├── Chart.yaml
│   ├── values.yaml                 ← Default values (2 replicas, HPA, probes)
│   └── templates/
│       ├── deployment.yaml         ← Rolling update, preStop drain hook
│       └── hpa.yaml                ← CPU + custom metrics autoscaling
│
├── k8s/
│   └── base/argocd-app.yaml       ← GitOps Application CR (prune + selfHeal)
│
├── observability/
│   ├── prometheus.yml              ← Scrape configs (static + k8s SD)
│   ├── rules/spring-alerts.yml    ← Alerting rules (uptime, latency, GC, CB)
│   └── grafana/provisioning/
│       ├── datasources/            ← Prometheus + Jaeger auto-provisioned
│       └── dashboards/
│           └── spring-overview.json ← 7-panel dashboard (latency, errors, JVM, orders)
│
└── docker-compose.yml              ← Full local stack (postgres, kafka, redis, all services)
```

---

## Quick start (local)

```bash
# 1. Start the full stack
docker-compose up -d

# 2. Wait for services to register (~30s), then test
curl -X POST "http://localhost:8080/api/orders?amount=99.99" \
     -H "Authorization: Bearer <JWT>"

# 3. Dashboards
open http://localhost:3000   # Grafana  (admin/admin)
open http://localhost:8761   # Eureka   (service registry)
open http://localhost:9090   # Prometheus
open http://localhost:16686  # Jaeger   (distributed traces)
open http://localhost:9093   # Kafka UI
```

---

## Building individual modules

```bash
# Build all modules
mvn clean package -DskipTests

# Build and push Docker image (Jib)
mvn jib:build -pl order-service

# Run order-service locally (requires infra running)
cd order-service
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

---

## Deploying to Kubernetes

```bash
# Install Helm chart
helm upgrade --install order-service ./helm/order-service \
  --namespace production \
  --create-namespace \
  --values helm/order-service/values.yaml \
  --set image.tag=1.0.0 \
  --set secrets.DB_PASSWORD=<secret>

# Apply ArgoCD GitOps app
kubectl apply -f k8s/base/argocd-app.yaml

# Check HPA
kubectl get hpa -n production

# Watch rolling deploy
kubectl rollout status deployment/order-service -n production
```

---

## Key design decisions

| Decision | Choice | Rationale |
|---|---|---|
| Service mesh | Kubernetes + Spring Cloud | Avoid Istio complexity for small clusters |
| Config source | Git-backed Config Server | Auditable, versioned, profile-aware |
| Service discovery | Eureka | Simpler than Consul for Spring-native stacks |
| Circuit breaking | Resilience4j | Native Spring Boot 3 integration, no proxy needed |
| Kafka reliability | Idempotent producer + DLQ | Exactly-once semantics without transactions overhead |
| Observability | Micrometer → Prometheus | Vendor-neutral, pull model, standard for k8s |
| Tracing | OpenTelemetry + Jaeger | W3C traceparent, no vendor lock-in |
| Container image | Layered JAR + eclipse-temurin | Fast rebuilds, smaller layers, JRE-only runtime |
| DB migrations | Flyway | Version-controlled, runs at startup, baseline-safe |

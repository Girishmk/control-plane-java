# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /build

# Cache Maven dependencies first (layer caching)
COPY pom.xml .
COPY config-server/pom.xml     config-server/pom.xml
COPY eureka-server/pom.xml     eureka-server/pom.xml
COPY api-gateway/pom.xml       api-gateway/pom.xml
COPY order-service/pom.xml     order-service/pom.xml

RUN apk add --no-cache maven && \
    mvn dependency:go-offline -B -q

# Copy source and build
COPY . .
RUN mvn package -pl order-service -am -DskipTests -B -q

# Extract Spring Boot layered JAR
WORKDIR /build/order-service/target
RUN java -Djarmode=layertools -jar order-service-*.jar extract

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy layered JAR contents — least → most frequently changed
COPY --from=builder --chown=appuser:appgroup /build/order-service/target/dependencies/          ./
COPY --from=builder --chown=appuser:appgroup /build/order-service/target/spring-boot-loader/   ./
COPY --from=builder --chown=appuser:appgroup /build/order-service/target/snapshot-dependencies/ ./
COPY --from=builder --chown=appuser:appgroup /build/order-service/target/application/          ./

USER appuser

EXPOSE 8081

# JVM flags:
#   UseContainerSupport   → respect cgroup memory limits
#   MaxRAMPercentage      → use 75% of container memory for heap
#   ExitOnOutOfMemoryError → crash fast so k8s can restart
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+ExitOnOutOfMemoryError \
               -XX:+UseG1GC \
               -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.JarLauncher"]

HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8081/actuator/health || exit 1

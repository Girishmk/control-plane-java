# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /build

COPY pom.xml .
COPY config-server/pom.xml     config-server/pom.xml
COPY eureka-server/pom.xml     eureka-server/pom.xml
COPY api-gateway/pom.xml       api-gateway/pom.xml
COPY order-service/pom.xml     order-service/pom.xml

RUN apt-get update && apt-get install -y --no-install-recommends maven && rm -rf /var/lib/apt/lists/*
RUN mvn dependency:go-offline -B -q

COPY . .
RUN mvn package -pl api-gateway -am -DskipTests -B -q

WORKDIR /build/api-gateway/target
RUN java -Djarmode=layertools -jar api-gateway-*.jar extract

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy AS runtime

RUN groupadd -r appgroup && useradd -r -g appgroup appuser

WORKDIR /app

COPY --from=builder --chown=appuser:appgroup /build/api-gateway/target/dependencies/          ./
COPY --from=builder --chown=appuser:appgroup /build/api-gateway/target/spring-boot-loader/   ./
COPY --from=builder --chown=appuser:appgroup /build/api-gateway/target/snapshot-dependencies/ ./
COPY --from=builder --chown=appuser:appgroup /build/api-gateway/target/application/          ./

USER appuser

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+ExitOnOutOfMemoryError \
               -XX:+UseG1GC \
               -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]

HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

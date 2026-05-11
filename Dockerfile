# =============================================================================
# Stage 1: Build
# =============================================================================
FROM eclipse-temurin:25-jdk AS builder

WORKDIR /app

# Cache Gradle wrapper and dependencies before copying source
COPY gradle gradle
COPY gradlew gradlew.bat settings.gradle build.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon --quiet

# Copy source and build the fat JAR
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# =============================================================================
# Stage 2: Runtime
# Minimal JRE image — no build tools, smaller attack surface
# =============================================================================
FROM eclipse-temurin:25-jre AS runtime

WORKDIR /app

# Run as non-root
RUN groupadd --system appgroup && \
    useradd --system --gid appgroup --no-create-home appuser

COPY --from=builder /app/build/libs/*.jar app.jar
RUN chown appuser:appgroup app.jar

USER appuser

# Tuned JVM flags for containerized environment:
#   -XX:+UseContainerSupport          — respect cgroup memory/cpu limits
#   -XX:MaxRAMPercentage=75.0         — use up to 75% of container memory for heap
#   -XX:+ExitOnOutOfMemoryError       — fail fast, let orchestrator restart
#   -Djava.security.egd=...           — faster SecureRandom (important for Spring startup)
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+ExitOnOutOfMemoryError \
               -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

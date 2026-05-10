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
# Minimal JRE Alpine image — no build tools, smaller attack surface
# =============================================================================
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Run as non-root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /app/build/libs/*.jar app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "--enable-preview", "-jar", "app.jar"]

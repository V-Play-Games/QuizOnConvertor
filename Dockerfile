# ==============================================================================
# QuizOnConvertor Multi-Stage Dockerfile
# ==============================================================================

# --- Stage 1: Build Stage ---
FROM eclipse-temurin:25-jdk AS builder

WORKDIR /app

# Copy Gradle wrapper and configuration files to leverage layer caching
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties ./

# Ensure gradlew script is executable
RUN chmod +x ./gradlew

# Resolve and cache dependencies
RUN ./gradlew dependencies --no-daemon

# Copy source files
COPY src src

# Build the fat executable JAR (omitting test execution during image build)
RUN ./gradlew buildFatJar --no-daemon -x test

# --- Stage 2: Runtime Stage ---
FROM eclipse-temurin:25-jre

WORKDIR /app

# Create output and images directories for local volume mounting
RUN mkdir -p /app/output /app/images

# Copy the compiled fat JAR from the builder stage
COPY --from=builder /app/build/libs/QuizOnConvertor-all.jar /app/app.jar

# Expose Ktor server port
EXPOSE 8080

# Set default environment variables
ENV PORT=8080

# Default entrypoint runs the Ktor Web GUI server mode
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
CMD ["--server", "--port", "8080"]

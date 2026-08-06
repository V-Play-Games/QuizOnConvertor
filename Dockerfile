# --- Stage 1: Build Stage ---
FROM gradle:9.6-jdk25 AS builder

WORKDIR /app

COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties ./

RUN --mount=type=cache,target=/root/.gradle \
    gradle dependencies --no-daemon

COPY src src

RUN --mount=type=cache,target=/root/.gradle \
    gradle buildFatJar --no-daemon --console=plain -x test

# --- Stage 2: Runtime Stage ---
FROM eclipse-temurin:25-jre

WORKDIR /app

RUN mkdir -p /app/output /app/images

COPY --from=builder /app/build/libs/QuizOnConvertor-all.jar /app/app.jar

EXPOSE 8080

ENV PORT=8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
CMD ["--server", "--port", "8080"]

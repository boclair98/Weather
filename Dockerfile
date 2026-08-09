FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /workspace
COPY gradle gradle
COPY gradlew build.gradle ./
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew dependencies --no-daemon

COPY src src
RUN ./gradlew bootJar --no-daemon && \
    cp "$(find build/libs -name '*.jar' ! -name '*-plain.jar' | head -n 1)" /workspace/app.jar

FROM eclipse-temurin:17-jre-alpine

LABEL org.opencontainers.image.title="Weather Decision Service" \
      org.opencontainers.image.source="https://github.com/boclair98/Weather" \
      org.opencontainers.image.description="Traceable KMA-based weather decision and notification service"

RUN addgroup -g 10001 -S weather && adduser -u 10001 -S weather -G weather
WORKDIR /app
COPY --from=build --chown=weather:weather /workspace/app.jar /app/app.jar

USER weather
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/app.jar"]

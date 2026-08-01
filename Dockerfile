FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /workspace
COPY gradle gradle
COPY gradlew build.gradle ./
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew dependencies --no-daemon

COPY src src
RUN ./gradlew bootJar --no-daemon && \
    cp "$(find build/libs -name '*.jar' ! -name '*-plain.jar' | head -n 1)" /workspace/app.jar

FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S weather && adduser -S weather -G weather
WORKDIR /app
COPY --from=build --chown=weather:weather /workspace/app.jar /app/app.jar

USER weather
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

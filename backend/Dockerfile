# syntax=docker/dockerfile:1
# Backend image: Maven builds the jar with JDK 21, then only a slim Java 21 runtime is kept.
# No credentials are in this file: the database settings arrive as environment variables at run time.

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
# Dependencies first, so that this layer is reused while only the source changes.
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
# The application does not need root.
RUN addgroup -S matriz && adduser -S matriz -G matriz
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
USER matriz
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

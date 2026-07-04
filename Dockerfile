FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /workspace
COPY gradle gradle
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY src src
RUN ./gradlew --no-daemon clean bootJar

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN addgroup -S aisly && adduser -S aisly -G aisly
COPY --from=build /workspace/build/libs/*.jar /app/aisly-backend.jar
USER aisly
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/aisly-backend.jar"]


FROM gradle:8.10.2-jdk17 AS build
WORKDIR /workspace
COPY gradle gradle
COPY gradlew .
COPY settings.gradle.kts build.gradle.kts ./
COPY src src
RUN chmod +x gradlew
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
ENV SPRING_PROFILES_ACTIVE=postgres
EXPOSE 8433
COPY --from=build /workspace/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]

FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /workspace

COPY gradlew ./gradlew
COPY gradle ./gradle
COPY payment-service/build.gradle.kts ./build.gradle.kts
COPY payment-service/settings.gradle.kts ./settings.gradle.kts
COPY payment-service/src ./src

RUN chmod +x ./gradlew
RUN ./gradlew --no-daemon bootJar && \
    app_jar="$(find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar')" && \
    cp "$app_jar" /tmp/app.jar

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

EXPOSE 8434

COPY --from=builder /tmp/app.jar /app/app.jar

ENTRYPOINT ["java","-jar","/app/app.jar"]

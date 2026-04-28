FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
EXPOSE 8434
COPY build/libs/*.jar /app/app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]

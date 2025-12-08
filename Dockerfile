FROM gradle:7.6.1-jdk17-alpine AS build
WORKDIR /app
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle .
COPY settings.gradle .
COPY src ./src
RUN chmod +x gradlew
RUN ./gradlew bootJar
FROM eclipse-temurin:17-jre-alpine
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8001
ENTRYPOINT ["java","-jar","/app.jar"]

# Stage 1: Build
# Base Image for Maven with Java 21
FROM maven:3.9-eclipse-temurin-21 AS build
# Define WORKDIR
WORKDIR /app
# Copy necessary Dependencies
COPY pom.xml .
# Copy Code
COPY src ./src
# Package app
RUN mvn clean package -DskipTests

# Stage 2: Runtime
# Base Image for Java 21
FROM eclipse-temurin:21
# Define WORKDIR
WORKDIR /app
# Copy .jar as app.jar
COPY --from=build /app/target/*.jar app.jar
# Expose app to port 8080
EXPOSE 8080
# start application
ENTRYPOINT ["java", "-jar", "app.jar"]
# STAGE 1: Build the application with Maven
FROM maven:3.9-eclipse-temurin-21 AS builder

LABEL maintainer="ShortsCreator Team"
LABEL service="gateway-service"

WORKDIR /app

# Copy the parent pom.xml to leverage Docker layer caching.
# Dependencies are only re-downloaded if the pom.xml changes.
COPY pom.xml .

# Copy all pom.xml files
COPY ./content-storage-service/pom.xml ./content-storage-service/
COPY ./shortscreator-shared/pom.xml ./shortscreator-shared/
COPY ./content-generation-service/pom.xml ./content-generation-service/
COPY ./gateway-service/pom.xml ./gateway-service/
COPY ./notification-service/pom.xml ./notification-service/
COPY ./payment-service/pom.xml ./payment-service/
COPY ./keycloak-event-listener/pom.xml ./keycloak-event-listener/

# Resolve dependencies for all modules before copying source code for better caching
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline

# Copy the CSS and shared module source code
COPY ./gateway-service ./gateway-service
COPY ./shortscreator-shared ./shortscreator-shared

# Build the specific 'gateway-service' module and its dependencies (-am)
RUN --mount=type=cache,target=/root/.m2 mvn clean package -pl gateway-service -am -DskipTests

# STAGE 2: Create the final production image
FROM eclipse-temurin:21-jre-jammy

# Create a non-root user and group for better security
RUN groupadd --system appuser && useradd --system --gid appuser appuser
WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /app/gateway-service/target/gateway-service-*.jar app.jar

# Change ownership of the application directory and JAR file to the non-root user
RUN chown -R appuser:appuser /app
USER appuser

# Expose the port the application will listen on (documentation only).
EXPOSE 8081

# Define the command to run the application when the container starts
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
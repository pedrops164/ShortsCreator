# =============================================
# STAGE 1: Build the application with Maven
# =============================================
# Use a base image with Java 21 and Maven pre-installed
FROM maven:3.9-eclipse-temurin-21 AS builder

# Add a label to the image to identify the maintainer
LABEL maintainer="ShortsCreator Team"
LABEL service="content-storage-service"

# Set the working directory inside the container
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
COPY ./content-storage-service ./content-storage-service
COPY ./shortscreator-shared ./shortscreator-shared

# Build the specific 'content-storage-service' module and its dependencies (-am).
# We skip tests as they should be run in a separate CI stage.
# Use the Maven cache mount for faster builds.
RUN --mount=type=cache,target=/root/.m2 mvn clean package -pl content-storage-service -am -DskipTests

# =============================================
# STAGE 2: Create the final production image
# =============================================
# Use a minimal, secure JRE image for the final stage
FROM eclipse-temurin:21-jre-jammy

# Certificate setup for AWS DocumentDB
# The JRE image is minimal, so install curl first.
# Run as root to install packages and modify the Java truststore.
RUN apt-get update && apt-get install -y curl && \
    # Download the AWS RDS CA certificate for DocumentDB
    curl -o /tmp/global-bundle.pem https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem && \
    # Import the certificate into the default Java truststore
    keytool -importcert \
        -alias "AWSRDS-DOCDB-ROOT-CA" \
        -file /tmp/global-bundle.pem \
        -keystore $JAVA_HOME/lib/security/cacerts \
        -storepass changeit \
        -noprompt && \
    # Clean up the downloaded certificate file and apt cache
    rm /tmp/global-bundle.pem && \
    rm -rf /var/lib/apt/lists/*

# Create a non-root user and group for better security
RUN groupadd --system appuser && useradd --system --gid appuser appuser

# Set the working directory for the final image
WORKDIR /app

# Copy the built JAR from the 'builder' stage into the final image
# Renaming it to 'app.jar' simplifies the run command.
COPY --from=builder /app/content-storage-service/target/content-storage-service-*.jar app.jar

# Change ownership of the application directory and JAR file to the non-root user
RUN chown -R appuser:appuser /app

# Switch to the non-root user
USER appuser

# Expose the port the application will listen on (documentation only).
EXPOSE 8083

# Define the command to run the application when the container starts
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
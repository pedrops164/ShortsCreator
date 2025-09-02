# --- Build Stage ---
# This stage pre-builds and optimizes the Keycloak server configuration.
FROM quay.io/keycloak/keycloak:26.2 AS builder

# Set build-time environment variables for optimization.
# These tell Keycloak what features to include in the optimized build.
ENV KC_DB=postgres
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true
# Setting proxy tells Keycloak to prepare for running behind a reverse proxy.
ENV KC_PROXY=edge

# Run the build command to create an optimized server image.
# This is slow, but it only runs once when the Docker image is built.
RUN /opt/keycloak/bin/kc.sh build

# --- Final Stage ---
# This is the final, lean image that will run in production.
FROM quay.io/keycloak/keycloak:26.2

# Copy the optimized server files and the realm import file from the builder stage.
COPY --from=builder /opt/keycloak/lib /opt/keycloak/lib
COPY --from=builder /opt/keycloak/providers /opt/keycloak/providers
COPY --from=builder /opt/keycloak/quarkus-run.jar /opt/keycloak/quarkus-run.jar
COPY ./keycloak-server/shorts-creator-realm.json /opt/keycloak/data/import/realm.json

# Set the default entrypoint.
ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start"]

# Expose the default HTTP port. Your ALB will connect to this port.
EXPOSE 8080
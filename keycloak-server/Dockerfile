# Use a specific version of the official Keycloak image for stable builds
FROM quay.io/keycloak/keycloak:26.2

# Add labels for better container management
LABEL maintainer="ShortsCreator Team"
LABEL service="keycloak-auth-server"

# Copy the pre-configured realm file into Keycloak's import directory.
# Keycloak will automatically detect and import this file on its first startup.
COPY ./keycloak-server/shorts-creator-realm.json /opt/keycloak/data/import/realm.json

# Expose Keycloak's default HTTP port
EXPOSE 8080

# Expose the management port
EXPOSE 9000

# Expose the HTTPS port (for future production use)
EXPOSE 8443

# Add the default command to start the server in development mode.
# This command will be executed when the container starts.
CMD ["start", "--import-realm", "--health-enabled=true", "--http-enabled=true", "--proxy=edge"]
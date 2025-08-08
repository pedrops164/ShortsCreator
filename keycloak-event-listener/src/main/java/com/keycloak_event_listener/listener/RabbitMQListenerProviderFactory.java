package com.keycloak_event_listener.listener;

import com.keycloak_event_listener.config.AppProperties;
import com.keycloak_event_listener.config.ConfigLoader;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class RabbitMQListenerProviderFactory implements EventListenerProviderFactory {

    private static final Logger logger = Logger.getLogger(RabbitMQListenerProviderFactory.class);
    public static final String PROVIDER_ID = "rabbitmq-user-creation-listener";

    private AppProperties props;
    private Connection rabbitmqConnection;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        // Pass the pre-initialized connection and properties to each listener instance
        return new RabbitMQUserCreationListener(rabbitmqConnection, props);
    }

    @Override
    public void init(Config.Scope config) {
        // This method is called once on startup
        try {
            this.props = ConfigLoader.load();
            logger.info("Loaded RabbitMQ event listener configuration.");
            initializeRabbitMQ();
        } catch (IOException | TimeoutException e) {
            logger.error("CRITICAL: Failed to initialize RabbitMQ connection for Keycloak Event Listener.", e);
            // We throw a runtime exception to prevent Keycloak from starting with a broken listener
            throw new RuntimeException("Failed to initialize RabbitMQ connection", e);
        }
    }

    private void initializeRabbitMQ() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(props.getRabbitmq().getHost());
        factory.setPort(props.getRabbitmq().getPort());
        factory.setUsername(props.getRabbitmq().getUsername());
        factory.setPassword(props.getRabbitmq().getPassword());
        factory.setVirtualHost(props.getRabbitmq().getVirtualHost());

        // Enable automatic connection recovery
        factory.setAutomaticRecoveryEnabled(true);
        // Recover topology (exchanges, queues, etc.)
        factory.setTopologyRecoveryEnabled(true);

        this.rabbitmqConnection = factory.newConnection();
        logger.info("RabbitMQ connection established for Keycloak Event Listener.");

        // Add a shutdown hook to gracefully close the connection
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // Not needed
    }

    @Override
    public void close() {
        // This is called when Keycloak shuts down
        try {
            if (rabbitmqConnection != null && rabbitmqConnection.isOpen()) {
                rabbitmqConnection.close();
                logger.info("RabbitMQ connection closed for Keycloak Event Listener.");
            }
        } catch (IOException e) {
            logger.error("Failed to close RabbitMQ connection.", e);
        }
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
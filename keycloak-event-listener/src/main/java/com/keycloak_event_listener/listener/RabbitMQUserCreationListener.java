package com.keycloak_event_listener.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keycloak_event_listener.config.AppProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.shortscreator.shared.dto.NewUserEvent;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class RabbitMQUserCreationListener implements EventListenerProvider {

    private static final Logger logger = Logger.getLogger(RabbitMQUserCreationListener.class);

    private final Connection connection;
    private final AppProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RabbitMQUserCreationListener(Connection connection, AppProperties props) {
        this.connection = connection;
        this.props = props;
    }

    @Override
    public void onEvent(Event event) {
        if (event.getType() != EventType.REGISTER) {
            return;
        }
        
        if (connection == null || !connection.isOpen()) {
            logger.error("Cannot publish NewUserEvent: RabbitMQ connection is not open.");
            // The automatic recovery should handle this, but it's good practice to check.
            return;
        }

        String userId = event.getUserId();
        logger.infof("New user registered in Keycloak with ID: %s", userId);
        NewUserEvent newUserEvent = new NewUserEvent(userId);

        // Use try-with-resources for the channel to ensure it's always closed
        try (Channel channel = connection.createChannel()) {
            String message = objectMapper.writeValueAsString(newUserEvent);
            
            // Ensure exchange exists before publishing (idempotent operation)
            channel.exchangeDeclare(props.getRabbitmq().getExchangeName(), "direct", true);

            channel.basicPublish(
                    props.getRabbitmq().getExchangeName(),
                    props.getRabbitmq().getNewUserRoutingKey(),
                    null,
                    message.getBytes(StandardCharsets.UTF_8)
            );
            logger.infof("Published NewUserEvent for userId: %s", userId);
        } catch (IOException | TimeoutException e) {
            logger.errorf(e, "Failed to serialize or publish NewUserEvent for userId: %s", userId);
        }
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        // Gracefully ignore admin events.
        // For example, creating a user via Admin API triggers this.
    }

    @Override
    public void close() {
        // The connection is managed by the factory, so nothing to do here.
    }
}
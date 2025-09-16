package com.content_generation_service.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Bean
    public HttpClient httpClient() {
        // Configure the connection provider for the connection pool
        ConnectionProvider provider = ConnectionProvider.builder("custom")
            .maxConnections(50)
            .pendingAcquireMaxCount(-1)
            // Evict (close) any connection that has been idle for more than 60 seconds.
            // This ensures we don't use stale connections dropped by firewalls or NAT gateways.
            .maxIdleTime(Duration.ofSeconds(60))
            .build();

        return HttpClient.create(provider)
            // --- GLOBAL TIMEOUTS ---
            // Max time to wait for a connection from the pool
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            // A read/write timeout applied to the entire request lifecycle
            .doOnConnected(conn -> conn
                .addHandlerLast(new ReadTimeoutHandler(30, TimeUnit.SECONDS))
                .addHandlerLast(new WriteTimeoutHandler(30, TimeUnit.SECONDS)));
    }

    @Bean
    public WebClient.Builder webClientBuilder(HttpClient httpClient) {
        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}
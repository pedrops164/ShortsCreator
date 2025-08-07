package com.content_storage_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    private final AppProperties appProperties;

    public WebClientConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Bean
    public WebClient paymentServiceWebClient() {
        return WebClient.builder()
                .baseUrl(appProperties.getServices().getPaymentService().getUrl())
                .build();
    }
}
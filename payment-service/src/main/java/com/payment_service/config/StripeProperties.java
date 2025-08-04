package com.payment_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "stripe")
public record StripeProperties(
    String secretKey,
    String webhookSecret,
    String successUrl,
    String cancelUrl,
    Map<String, String> priceMap
) {}
package com.gateway_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.http.HttpMethod;

/**
 * Security configuration for the API Gateway.
 * @EnableWebFluxSecurity is used because Spring Cloud Gateway is built on Spring WebFlux (reactive stack).
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * Configures the security filter chain for all requests passing through the gateway.
     *
     * @param http The ServerHttpSecurity to configure.
     * @return The configured SecurityWebFilterChain.
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            // Configure CORS from the frontend application.
            .cors(Customizer.withDefaults())
            // Define authorization rules for all requests.
            .authorizeExchange(exchanges -> exchanges
                // Permit all OPTIONS preflight requests (For CORS and other preflight checks)
                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll() 
                // Allow unauthenticated access to the Stripe webhook endpoint
                .pathMatchers("/api/v1/stripe/webhooks").permitAll()
                .pathMatchers("/api/v1/assets/**").permitAll()
                .pathMatchers("/api/v1/presets/**").permitAll()
                .pathMatchers("/actuator/health").permitAll()
                // All other requests must be authenticated
                .anyExchange().authenticated()
            )
            // Configure the gateway as an OAuth2 Resource Server using JWT.
            // This will automatically trigger JWT validation for requests with an Authorization header.
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        
        // Disable CSRF protection for stateless APIs (common for gateways).
        http.csrf(ServerHttpSecurity.CsrfSpec::disable);

        return http.build();
    }
}

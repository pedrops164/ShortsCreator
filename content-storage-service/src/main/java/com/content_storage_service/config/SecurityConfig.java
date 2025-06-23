package com.content_storage_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;

import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.Customizer;

@Configuration
@EnableWebFluxSecurity // Enable WebFlux security for reactive applications
@EnableReactiveMethodSecurity // Enable method-level security for reactive applications
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            .authorizeExchange(exchanges -> exchanges
                // Still require that a valid principal exists for these paths
                .pathMatchers("/api/v1/content/**").authenticated()
                .anyExchange().permitAll()
            )
            .csrf(csrf -> csrf.disable())
            // Configure this service as a resource server that can parse the JWT
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }
}
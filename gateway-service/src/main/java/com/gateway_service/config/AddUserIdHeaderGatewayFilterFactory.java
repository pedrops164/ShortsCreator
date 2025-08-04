package com.gateway_service.config;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class AddUserIdHeaderGatewayFilterFactory 
    extends AbstractGatewayFilterFactory<Object> { // We don't need a config object

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            return exchange.getPrincipal()
                .flatMap(principal -> {
                    if (principal instanceof JwtAuthenticationToken jwtAuth) {
                        Jwt jwt = jwtAuth.getToken();
                        String userId = jwt.getSubject(); // 'sub' claim

                        // Create a new request with the added header
                        ServerHttpRequest request = exchange.getRequest().mutate()
                            .header("X-User-ID", userId)
                            .build();
                        
                        return chain.filter(exchange.mutate().request(request).build());
                    }
                    return chain.filter(exchange);
                })
                // If there's no principal, just continue the chain without modification
                .switchIfEmpty(chain.filter(exchange));
        };
    }
}
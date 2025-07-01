package com.gateway_service.config;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.http.server.reactive.ServerHttpRequest;

/**
 * A global filter that runs after authentication to extract user identity
 * from the JWT and add it as a custom header to the downstream request.
 */
@Component
public class IdentityHeaderFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
            // The principal should be a JwtAuthenticationToken after our security config runs
            .flatMap(principal -> {
                if (principal instanceof JwtAuthenticationToken) {
                    JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) principal;
                    Jwt jwt = jwtAuth.getToken();
                    String userId = jwt.getSubject(); // 'sub' claim is the user's unique ID

                    // Create a new request with the added user ID header
                    ServerHttpRequest request = exchange.getRequest().mutate()
                        .header("X-User-ID", userId)
                        .build();
                    // Pass a new exchange object with the mutated request to the filter chain.
                    return chain.filter(exchange.mutate().request(request).build());
                }
                return chain.filter(exchange);
            })
            // If there's no principal, just continue the chain
            .switchIfEmpty(chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        // Run this filter *after* the main security authentication filter
        return -1;
    }
}

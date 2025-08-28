package com.gateway_service.config;

import java.util.List;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;

@Component
public class AddUserIdHeaderGatewayFilterFactory 
    extends AbstractGatewayFilterFactory<AddUserIdHeaderGatewayFilterFactory.Config> {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public AddUserIdHeaderGatewayFilterFactory() {
        super(Config.class);
    }
    
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();

            // Check if the path matches any of the configured patterns.
            // If 'onlyOnPaths' is not specified in YAML, it applies the header to all paths.
            boolean shouldApplyFilter = config.getOnlyOnPaths() == null || config.getOnlyOnPaths().isEmpty() ||
                config.getOnlyOnPaths().stream().anyMatch(pattern -> pathMatcher.match(pattern, path));

            if (!shouldApplyFilter) {
                // If the path doesn't match, continue the chain without modification.
                return chain.filter(exchange);
            }

            // If the path matches, proceed with the original logic to add the header.
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

    // This nested class holds the configuration from your application.yml
    @Validated
    public static class Config {
        private List<String> onlyOnPaths;

        public List<String> getOnlyOnPaths() {
            return onlyOnPaths;
        }

        public void setOnlyOnPaths(List<String> onlyOnPaths) {
            this.onlyOnPaths = onlyOnPaths;
        }
    }
}
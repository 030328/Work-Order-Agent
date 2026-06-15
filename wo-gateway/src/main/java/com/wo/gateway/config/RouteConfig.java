package com.wo.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("wo-service-user", r -> r
                        .path("/api/user/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://wo-service-user"))
                .route("wo-service-workorder", r -> r
                        .path("/api/workorder/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://wo-service-workorder"))
                .route("wo-service-workflow", r -> r
                        .path("/api/workflow/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://wo-service-workflow"))
                .route("wo-service-ai-agent", r -> r
                        .path("/api/ai/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://wo-service-ai-agent"))
                .build();
    }
}

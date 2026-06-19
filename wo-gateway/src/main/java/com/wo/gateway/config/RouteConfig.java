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
                // 用户服务 - 登录/注册
                .route("wo-service-auth", r -> r
                        .path("/api/auth/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://wo-service-user"))
                // 用户服务 - 用户管理
                .route("wo-service-user", r -> r
                        .path("/api/users/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://wo-service-user"))
                // 工单服务
                .route("wo-service-workorder", r -> r
                        .path("/api/workorders/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://wo-service-workorder"))
                // 工作流服务
                .route("wo-service-workflow", r -> r
                        .path("/api/workflow/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://wo-service-workflow"))
                // AI Agent 服务
                .route("wo-service-ai-agent", r -> r
                        .path("/api/ai/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://wo-service-ai-agent"))
                .build();
    }
}

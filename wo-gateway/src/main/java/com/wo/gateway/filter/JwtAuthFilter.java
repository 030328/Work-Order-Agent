package com.wo.gateway.filter;

import com.wo.common.constant.CommonConstant;
import com.wo.common.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /**
     * Whitelist paths that do not require JWT authentication.
     */
    private static final List<String> WHITELIST = Arrays.asList(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/api/workorders/public/**",
            "/api/workflow/public/**",
            "/api/ai/public/**",
            "/actuator/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/webjars/**"
    );

    /**
     * Service-to-service endpoints must not be exposed through the public gateway.
     */
    private static final List<String> INTERNAL_ONLY_PATHS = Arrays.asList(
            "/api/workorders/internal/**",
            "/api/workflow/transitions/**",
            "/api/workflow/sla/**",
            "/api/workflow/definitions/**"
    );

    @Override
    public int getOrder() {
        return -100;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isInternalOnlyPath(path)) {
            return forbiddenResponse(exchange, "Internal endpoint is not exposed through gateway");
        }

        // Check if the path is in the whitelist
        if (isWhitelistPath(path)) {
            return chain.filter(exchange);
        }

        // Extract JWT from Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorizedResponse(exchange, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);

        // Validate JWT and extract claims
        try {
            Map<String, Object> claims = JwtUtil.parseToken(token);
            if (claims == null) {
                return unauthorizedResponse(exchange, "Invalid or expired token");
            }

            String userId = String.valueOf(claims.get("userId"));
            String username = String.valueOf(claims.get("username"));
            String role = String.valueOf(claims.get("role"));

            // Strip any client-supplied identity headers before adding trusted JWT claims.
            ServerHttpRequest mutatedRequest = request.mutate()
                    .headers(headers -> {
                        headers.remove(CommonConstant.USER_ID_HEADER);
                        headers.remove(CommonConstant.USERNAME_HEADER);
                        headers.remove(CommonConstant.ROLE_HEADER);
                        headers.remove(CommonConstant.INTERNAL_SERVICE_TOKEN_HEADER);
                    })
                    .header(CommonConstant.USER_ID_HEADER, userId != null ? userId : "")
                    .header(CommonConstant.USERNAME_HEADER, username != null ? username : "")
                    .header(CommonConstant.ROLE_HEADER, role != null ? role : "")
                    .build();

            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(mutatedRequest)
                    .build();

            return chain.filter(mutatedExchange);
        } catch (Exception e) {
            log.error("JWT validation failed for path: {}", path, e);
            return unauthorizedResponse(exchange, "Token validation failed: " + e.getMessage());
        }
    }

    /**
     * Check if the given path matches any whitelist pattern.
     */
    private boolean isWhitelistPath(String path) {
        return WHITELIST.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    private boolean isInternalOnlyPath(String path) {
        return INTERNAL_ONLY_PATHS.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    /**
     * Return a 401 Unauthorized response with a JSON body.
     */
    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        return jsonErrorResponse(exchange, HttpStatus.UNAUTHORIZED, 401, message);
    }

    private Mono<Void> forbiddenResponse(ServerWebExchange exchange, String message) {
        return jsonErrorResponse(exchange, HttpStatus.FORBIDDEN, 403, message);
    }

    private Mono<Void> jsonErrorResponse(ServerWebExchange exchange, HttpStatus status, int code, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format(
                "{\"code\":%d,\"message\":\"%s\",\"data\":null}",
                code,
                message.replace("\"", "\\\"")
        );
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}

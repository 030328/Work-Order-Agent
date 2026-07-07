package com.wo.user.config;

import com.wo.common.constant.CommonConstant;
import com.wo.common.security.InternalServiceAuth;
import com.wo.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Filter that reads user info headers set by the API gateway
 * and populates the Spring Security context.
 */
@Component
@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {

    private final InternalServiceAuth internalServiceAuth;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip authentication for auth endpoints
        if (path.startsWith("/api/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (authenticateByJwt(request) || authenticateInternalService(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean authenticateByJwt(HttpServletRequest request) {
        String header = request.getHeader(CommonConstant.TOKEN_HEADER);
        if (!StringUtils.hasText(header) || !header.startsWith(CommonConstant.TOKEN_PREFIX)) {
            return false;
        }

        try {
            Claims claims = JwtUtil.parseToken(header.substring(CommonConstant.TOKEN_PREFIX.length()));
            String userId = String.valueOf(claims.get("userId"));
            String username = claims.get("username", String.class);
            String role = claims.get("role", String.class);
            setAuthentication(userId, username, role);
            return true;
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            return false;
        }
    }

    private boolean authenticateInternalService(HttpServletRequest request) {
        if (!internalServiceAuth.isInternalRequest(request)) {
            return false;
        }
        setAuthentication("0", "internal-service", "SYSTEM");
        return true;
    }

    private void setAuthentication(String userId, String username, String role) {
        List<SimpleGrantedAuthority> authorities = Collections.emptyList();
        if (StringUtils.hasText(role)) {
            authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);
        authentication.setDetails(username);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}

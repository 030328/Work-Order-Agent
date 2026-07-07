package com.wo.common.util;

import com.wo.common.constant.CommonConstant;
import com.wo.common.enums.ErrorCode;
import com.wo.common.exception.BizException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class SecurityUtil {

    private SecurityUtil() {}

    public static Long getCurrentUserId() {
        Claims claims = getTokenClaims();
        return claims != null ? claims.get("userId", Long.class) : null;
    }

    public static String getCurrentUsername() {
        Claims claims = getTokenClaims();
        return claims != null ? claims.get("username", String.class) : null;
    }

    public static String getCurrentRole() {
        Claims claims = getTokenClaims();
        return claims != null ? claims.get("role", String.class) : null;
    }

    public static Long requireCurrentUserId() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    public static String getToken() {
        HttpServletRequest request = getRequest();
        if (request == null) return null;
        String header = request.getHeader(CommonConstant.TOKEN_HEADER);
        if (header != null && header.startsWith(CommonConstant.TOKEN_PREFIX)) {
            return header.substring(CommonConstant.TOKEN_PREFIX.length());
        }
        return header;
    }

    public static boolean hasRequestContext() {
        return getRequest() != null;
    }

    private static Claims getTokenClaims() {
        String token = getToken();
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return JwtUtil.parseToken(token);
        } catch (Exception e) {
            return null;
        }
    }

    public static HttpServletRequest getRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }
}

package com.wo.common.util;

import com.wo.common.constant.CommonConstant;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class SecurityUtil {

    private SecurityUtil() {}

    public static Long getCurrentUserId() {
        HttpServletRequest request = getRequest();
        if (request == null) return null;
        String userId = request.getHeader(CommonConstant.USER_ID_HEADER);
        return userId != null ? Long.parseLong(userId) : null;
    }

    public static String getCurrentUsername() {
        HttpServletRequest request = getRequest();
        if (request == null) return null;
        return request.getHeader(CommonConstant.USERNAME_HEADER);
    }

    public static String getCurrentRole() {
        HttpServletRequest request = getRequest();
        if (request == null) return null;
        return request.getHeader(CommonConstant.ROLE_HEADER);
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

    private static HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}

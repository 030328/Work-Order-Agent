package com.wo.agent.config;

import com.wo.common.constant.CommonConstant;
import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@Configuration
public class FeignHeaderRelayConfig {

    private static final String SYSTEM_USER_ID = "0";
    private static final String SYSTEM_USERNAME = "ai-agent";
    private static final String SYSTEM_ROLE = "SYSTEM";

    private static final List<String> RELAY_HEADERS = List.of(
            CommonConstant.TOKEN_HEADER,
            CommonConstant.TRACE_ID_HEADER,
            CommonConstant.USER_ID_HEADER,
            CommonConstant.USERNAME_HEADER,
            CommonConstant.ROLE_HEADER
    );

    @Bean
    public RequestInterceptor userContextRelayInterceptor() {
        return template -> {
            HttpServletRequest request = currentRequest();
            if (request != null) {
                for (String header : RELAY_HEADERS) {
                    String value = request.getHeader(header);
                    if (StringUtils.hasText(value)) {
                        template.header(header, value);
                    }
                }
            }

            if (!template.headers().containsKey(CommonConstant.USER_ID_HEADER)) {
                template.header(CommonConstant.USER_ID_HEADER, SYSTEM_USER_ID);
            }
            if (!template.headers().containsKey(CommonConstant.USERNAME_HEADER)) {
                template.header(CommonConstant.USERNAME_HEADER, SYSTEM_USERNAME);
            }
            if (!template.headers().containsKey(CommonConstant.ROLE_HEADER)) {
                template.header(CommonConstant.ROLE_HEADER, SYSTEM_ROLE);
            }
        };
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }
}

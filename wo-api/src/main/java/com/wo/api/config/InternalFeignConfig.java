package com.wo.api.config;

import com.wo.common.constant.CommonConstant;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class InternalFeignConfig {

    @Value("${wo.internal-service-token:wo-internal-dev-token}")
    private String internalServiceToken;

    @Bean
    public RequestInterceptor internalServiceTokenInterceptor() {
        return template -> template.header(CommonConstant.INTERNAL_SERVICE_TOKEN_HEADER, internalServiceToken);
    }
}

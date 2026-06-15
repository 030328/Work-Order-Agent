package com.wo.agent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * SSE (Server-Sent Events) 配置
 * 配置异步请求超时时间，支持长连接的流式响应
 */
@Slf4j
@Configuration
public class SseConfig implements WebMvcConfigurer {

    private static final long SSE_TIMEOUT = 300_000L; // 5分钟超时

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setDefaultTimeout(SSE_TIMEOUT);
        log.info("SSE async timeout configured: {} ms", SSE_TIMEOUT);
    }
}

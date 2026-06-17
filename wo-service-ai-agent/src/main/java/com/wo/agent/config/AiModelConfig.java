package com.wo.agent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * AI 模型配置
 * 配置 DashScope 模型参数
 */
@Slf4j
@Configuration
public class AiModelConfig {

    @Value("${agent.model:qwen-plus}")
    private String modelName;

    @Value("${agent.max-tokens:2000}")
    private int maxTokens;

    @Value("${agent.temperature:0.7}")
    private double temperature;

    @Value("${spring.ai.dashscope.api-key:}")
    private String apiKey;

    /**
     * 初始化时打印配置信息
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("AI Model Config - model: {}, maxTokens: {}, temperature: {}",
                modelName, maxTokens, temperature);
        log.info("DashScope API Key configured: {}", apiKey != null && !apiKey.isEmpty() ? "yes" : "no");
    }

    public String getModelName() {
        return modelName;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public double getTemperature() {
        return temperature;
    }
}

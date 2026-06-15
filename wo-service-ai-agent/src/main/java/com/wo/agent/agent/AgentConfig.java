package com.wo.agent.agent;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI Agent 配置
 * 配置 ChatClient 和 DashScope 模型参数
 */
@Configuration
public class AgentConfig {

    @Value("${agent.model:qwen-plus}")
    private String modelName;

    @Value("${agent.max-tokens:2000}")
    private int maxTokens;

    @Value("${agent.temperature:0.7}")
    private double temperature;

    /**
     * 构建 ChatClient.Builder
     * 使用 DashScope 作为 AI 模型提供者
     *
     * @param dashScopeChatModel DashScope 聊天模型
     * @return ChatClient.Builder
     */
    @Bean
    public ChatClient.Builder chatClientBuilder(DashScopeChatModel dashScopeChatModel) {
        return ChatClient.builder(dashScopeChatModel);
    }
}

package com.wo.agent.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP (Model Context Protocol) Server 配置
 * 配置 MCP Server 以暴露 AI Agent 的工具能力
 *
 * Spring AI MCP Server 会自动发现并注册 @Tool 注解的 Bean
 * 其他 AI 客户端可以通过 MCP 协议调用这些工具
 */
@Slf4j
@Configuration
public class McpServerConfig {

    /**
     * 配置 MCP 工具回调提供者
     * 将所有 @Tool 注解的 Bean 暴露为 MCP 工具
     *
     * @param toolBeans 工具 Bean 数组
     * @return ToolCallbackProvider
     */
    @Bean
    public ToolCallbackProvider mcpToolCallbackProvider(Object... toolBeans) {
        log.info("Configuring MCP Server with {} tool beans", toolBeans.length);
        return MethodToolCallbackProvider.builder()
                .toolObjects(toolBeans)
                .build();
    }
}

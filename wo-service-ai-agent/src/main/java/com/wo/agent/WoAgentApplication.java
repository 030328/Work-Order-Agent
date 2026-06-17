package com.wo.agent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * AI Agent 微服务启动类
 * 核心功能：ReAct Agent、Tool Calling、RAG、MCP Server、会话记忆
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.wo.api.client")
@MapperScan("com.wo.agent.mapper")
@ComponentScan(basePackages = {"com.wo.agent", "com.wo.common"})
public class WoAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(WoAgentApplication.class, args);
    }
}

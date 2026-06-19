package com.wo.agent.service;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.wo.agent.tool.WorkOrderTools;
import com.wo.api.dto.ai.ChatResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    @Value("${dashscope.api-key}")
    private String apiKey;

    @Value("${agent.model:qwen-plus}")
    private String model;

    private final WorkOrderTools workOrderTools;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int MAX_TOOL_CALLS = 3;

    private static final String SYSTEM_PROMPT = """
            你是一个智能工单管理助手。你可以帮助用户管理工单。

            当用户需要执行操作时，请返回一个JSON格式的工具调用：
            ```json
            {"tool": "工具名", "args": {参数}}
            ```

            可用工具：
            1. createWorkOrder(title, description, category, priority) - 创建工单
            2. searchWorkOrders(keyword, status, priority) - 查询工单
            3. getWorkOrderDetail(workOrderId) - 获取工单详情
            4. updateWorkOrderStatus(workOrderId, status, comment) - 更新工单状态
            5. searchKnowledgeBase(query) - 搜索知识库
            6. listAvailableAgents(role) - 获取可用处理人

            如果用户只是聊天或询问，直接回复文本即可，不要返回JSON。
            回复要简洁友好。
            """;

    /**
     * 处理用户消息
     */
    public ChatResponse chat(String userMessage, List<Message> history) {
        log.info("Chat: userMessage={}", userMessage);

        List<Message> messages = new ArrayList<>();
        messages.add(Message.builder().role(Role.SYSTEM.getValue()).content(SYSTEM_PROMPT).build());

        if (history != null) {
            messages.addAll(history);
        }

        messages.add(Message.builder().role(Role.USER.getValue()).content(userMessage).build());

        int toolCallCount = 0;
        while (toolCallCount < MAX_TOOL_CALLS) {
            try {
                GenerationParam param = GenerationParam.builder()
                        .apiKey(apiKey)
                        .model(model)
                        .messages(messages)
                        .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                        .maxTokens(1000)
                        .temperature(0.7F)
                        .build();

                Generation gen = new Generation();
                GenerationResult result = gen.call(param);

                String content = result.getOutput().getChoices().get(0).getMessage().getContent();
                log.info("AI response: {}", content);

                // 检查是否是工具调用
                if (isToolCall(content)) {
                    toolCallCount++;
                    String toolResult = executeToolCall(content);

                    // 添加 AI 回复和工具结果到消息列表
                    messages.add(Message.builder().role(Role.ASSISTANT.getValue()).content(content).build());
                    messages.add(Message.builder().role(Role.USER.getValue())
                            .content("工具执行结果：" + toolResult + "\n请根据结果回复用户。").build());
                    continue;
                }

                // 普通回复
                return ChatResponse.builder()
                        .content(content)
                        .role("assistant")
                        .finished(true)
                        .build();
            } catch (Exception e) {
                log.error("AI call failed", e);
                return ChatResponse.builder()
                        .content("AI服务调用失败：" + e.getMessage())
                        .role("assistant")
                        .finished(true)
                        .build();
            }
        }

        return ChatResponse.builder()
                .content("抱歉，处理过程中出现问题，请稍后重试。")
                .role("assistant")
                .finished(true)
                .build();
    }

    private boolean isToolCall(String content) {
        return content != null && content.contains("\"tool\"") && content.contains("\"args\"");
    }

    private String executeToolCall(String jsonContent) {
        try {
            // 提取 JSON 部分
            String json = extractJson(jsonContent);
            JsonNode node = objectMapper.readTree(json);
            String toolName = node.get("tool").asText();
            JsonNode args = node.get("args");

            log.info("Executing tool: {}", toolName);

            return switch (toolName) {
                case "createWorkOrder" -> workOrderTools.createWorkOrder(
                        args.get("title").asText(),
                        args.get("description").asText(),
                        args.has("category") ? args.get("category").asText() : "QUESTION",
                        args.has("priority") ? args.get("priority").asText() : "MEDIUM"
                );
                case "searchWorkOrders" -> workOrderTools.searchWorkOrders(
                        args.has("keyword") ? args.get("keyword").asText() : "",
                        args.has("status") ? args.get("status").asText() : "",
                        args.has("priority") ? args.get("priority").asText() : ""
                );
                case "getWorkOrderDetail" -> workOrderTools.getWorkOrderDetail(
                        args.get("workOrderId").asLong()
                );
                case "updateWorkOrderStatus" -> workOrderTools.updateWorkOrderStatus(
                        args.get("workOrderId").asLong(),
                        args.get("status").asText(),
                        args.has("comment") ? args.get("comment").asText() : ""
                );
                case "searchKnowledgeBase" -> workOrderTools.searchKnowledgeBase(
                        args.get("query").asText()
                );
                case "listAvailableAgents" -> workOrderTools.listAvailableAgents(
                        args.has("role") ? args.get("role").asText() : "AGENT"
                );
                default -> "未知工具: " + toolName;
            };
        } catch (Exception e) {
            log.error("Tool execution failed", e);
            return "工具执行失败: " + e.getMessage();
        }
    }

    private String extractJson(String content) {
        int start = content.indexOf("{");
        int end = content.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        return content;
    }
}

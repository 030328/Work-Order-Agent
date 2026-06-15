package com.wo.agent.agent;

import com.wo.agent.memory.ConversationMemory;
import com.wo.agent.memory.SessionManager;
import com.wo.agent.rag.KnowledgeSearchResult;
import com.wo.agent.rag.RagService;
import com.wo.agent.tool.ToolRegistry;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 工单智能助手 - ReAct Agent 核心编排器
 *
 * 实现 ReAct (Reasoning + Acting) 循环：
 * 1. Perceive - 感知用户意图和上下文
 * 2. Reason - 推理下一步行动
 * 3. Act - 调用工具执行操作
 * 4. Observe - 观察执行结果
 * 5. Respond - 生成最终回复
 *
 * 集成能力：
 * - Tool Calling：通过 Spring AI 调用外部工具
 * - RAG：检索增强生成，从知识库获取相关信息
 * - 会话记忆：基于 Redis 的对话历史管理
 * - 流式输出：通过 SSE 实时返回响应
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderAgent {

    private final ChatClient.Builder chatClientBuilder;
    private final ConversationMemory conversationMemory;
    private final SessionManager sessionManager;
    private final RagService ragService;
    private final PromptTemplate promptTemplate;
    private final ToolRegistry toolRegistry;

    /**
     * 处理用户消息并返回流式响应
     *
     * ReAct 循环流程：
     * 1. 加载会话历史记忆
     * 2. 构建系统提示词（包含工单上下文、RAG 结果）
     * 3. 调用 ChatClient 执行 Tool Calling
     * 4. 流式返回响应
     * 5. 保存助手消息到记忆
     *
     * @param sessionId   会话ID
     * @param userMessage 用户消息
     * @param workOrderId 关联工单ID（可选）
     * @return 流式 ChatResponse
     */
    public Flux<ChatResponse> chat(String sessionId, String userMessage, Long workOrderId) {
        log.info("Agent chat - sessionId: {}, workOrderId: {}, message length: {}",
                sessionId, workOrderId, userMessage.length());

        // ===== Step 1: Perceive - 感知上下文 =====
        // 加载对话历史
        List<String> historyMessages = conversationMemory.loadMessages(sessionId);
        log.debug("Loaded {} history messages for session: {}", historyMessages.size(), sessionId);

        // RAG 检索相关知识
        List<KnowledgeSearchResult> ragResults = ragService.retrieve(userMessage, 5);
        log.debug("RAG retrieved {} results", ragResults.size());

        // ===== Step 2: Reason - 构建提示词 =====
        String systemPrompt = buildSystemPrompt(workOrderId, historyMessages, ragResults);
        log.debug("System prompt built, length: {}", systemPrompt.length());

        // 保存用户消息到记忆
        conversationMemory.saveMessage(sessionId, "user", userMessage);
        sessionManager.updateSessionActivity(sessionId);

        // ===== Step 3 & 4: Act + Observe - 调用 LLM 并获取工具调用结果 =====
        // 构建 ChatClient 并注册工具
        ChatClient chatClient = chatClientBuilder
                .defaultSystem(systemPrompt)
                .defaultTools(toolRegistry.getAllTools())
                .build();

        // ===== Step 5: Respond - 流式返回响应 =====
        Flux<ChatResponse> responseFlux = chatClient.prompt()
                .user(userMessage)
                .stream()
                .chatResponse()
                .doOnNext(response -> {
                    // 实时处理每个响应块
                    if (response.getResult() != null && response.getResult().getOutput() != null) {
                        AssistantMessage output = response.getResult().getOutput();
                        log.debug("Received response chunk, toolCalls: {}",
                                output.getToolCalls() != null ? output.getToolCalls().size() : 0);
                    }
                })
                .doOnComplete(() -> {
                    // 响应完成后保存助手消息
                    log.info("Chat response completed for session: {}", sessionId);
                    // 注意：完整的助手消息会在流结束后通过回调保存
                })
                .doOnError(error -> {
                    log.error("Chat error for session: {}", sessionId, error);
                });

        return responseFlux;
    }

    /**
     * 同步聊天（非流式），适用于需要完整响应的场景
     *
     * @param sessionId   会话ID
     * @param userMessage 用户消息
     * @param workOrderId 关联工单ID（可选）
     * @return 完整的助手回复
     */
    public String chatSync(String sessionId, String userMessage, Long workOrderId) {
        log.info("Agent sync chat - sessionId: {}, workOrderId: {}", sessionId, workOrderId);

        // 加载上下文
        List<String> historyMessages = conversationMemory.loadMessages(sessionId);
        List<KnowledgeSearchResult> ragResults = ragService.retrieve(userMessage, 5);
        String systemPrompt = buildSystemPrompt(workOrderId, historyMessages, ragResults);

        // 保存用户消息
        conversationMemory.saveMessage(sessionId, "user", userMessage);
        sessionManager.updateSessionActivity(sessionId);

        // 构建 ChatClient
        ChatClient chatClient = chatClientBuilder
                .defaultSystem(systemPrompt)
                .defaultTools(toolRegistry.getAllTools())
                .build();

        // 同步调用（LLM 会自动进行多轮 Tool Calling）
        String response = chatClient.prompt()
                .user(userMessage)
                .call()
                .content();

        // 保存助手回复
        conversationMemory.saveMessage(sessionId, "assistant", response);
        log.info("Sync chat completed for session: {}, response length: {}", sessionId, response.length());

        return response;
    }

    /**
     * 构建系统提示词，整合上下文信息
     */
    private String buildSystemPrompt(Long workOrderId, List<String> historyMessages,
                                      List<KnowledgeSearchResult> ragResults) {
        StringBuilder contextBuilder = new StringBuilder();

        // 工单上下文
        if (workOrderId != null) {
            contextBuilder.append("当前关联工单ID: ").append(workOrderId).append("\n");
        }

        // RAG 上下文
        if (ragResults != null && !ragResults.isEmpty()) {
            String ragContext = promptTemplate.buildRagContext(ragResults);
            contextBuilder.append(ragContext);
        }

        // 历史对话上下文
        if (historyMessages != null && !historyMessages.isEmpty()) {
            String memoryContext = promptTemplate.buildMemoryContext(historyMessages);
            contextBuilder.append(memoryContext);
        }

        return promptTemplate.buildSystemPrompt(contextBuilder.toString());
    }
}

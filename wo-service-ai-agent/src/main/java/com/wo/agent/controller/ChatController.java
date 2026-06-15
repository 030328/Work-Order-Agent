package com.wo.agent.controller;

import com.wo.agent.agent.WorkOrderAgent;
import com.wo.agent.memory.SessionManager;
import com.wo.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * AI 聊天控制器
 * 提供流式聊天接口
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class ChatController {

    private final WorkOrderAgent workOrderAgent;
    private final SessionManager sessionManager;

    /**
     * 流式聊天接口（SSE）
     *
     * @param sessionId   会话ID（可选，不传则自动创建）
     * @param message     用户消息
     * @param workOrderId 关联工单ID（可选）
     * @return 流式响应
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponse> chat(
            @RequestParam(required = false) String sessionId,
            @RequestParam String message,
            @RequestParam(required = false) Long workOrderId) {

        log.info("Chat request - sessionId: {}, workOrderId: {}, message: {}",
                sessionId, workOrderId, message.substring(0, Math.min(50, message.length())));

        // 如果没有会话ID，创建新会话
        if (sessionId == null || sessionId.isEmpty()) {
            // TODO: 从 SecurityContext 获取当前用户ID
            Long userId = 1L; // 临时默认值
            sessionId = sessionManager.createSession(userId, workOrderId);
            log.info("Created new session: {}", sessionId);
        }

        return workOrderAgent.chat(sessionId, message, workOrderId);
    }

    /**
     * 同步聊天接口（非流式）
     *
     * @param sessionId   会话ID
     * @param message     用户消息
     * @param workOrderId 关联工单ID（可选）
     * @return 完整回复
     */
    @PostMapping("/chat")
    public Result<String> chatSync(
            @RequestParam(required = false) String sessionId,
            @RequestParam String message,
            @RequestParam(required = false) Long workOrderId) {

        log.info("Sync chat request - sessionId: {}, workOrderId: {}", sessionId, workOrderId);

        // 如果没有会话ID，创建新会话
        if (sessionId == null || sessionId.isEmpty()) {
            Long userId = 1L;
            sessionId = sessionManager.createSession(userId, workOrderId);
        }

        String response = workOrderAgent.chatSync(sessionId, message, workOrderId);
        return Result.success(response);
    }
}

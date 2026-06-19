package com.wo.agent.controller;

import com.alibaba.dashscope.common.Message;
import com.wo.agent.service.ChatService;
import com.wo.api.dto.ai.ChatRequest;
import com.wo.api.dto.ai.ChatResponse;
import com.wo.common.result.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 会话历史缓存（生产环境用 Redis）
     */
    private final Map<String, List<Message>> sessionHistory = new ConcurrentHashMap<>();

    /**
     * 对话接口
     */
    @PostMapping("/chat")
    public R<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("Chat request: sessionId={}, message={}", request.getSessionId(), request.getMessage());

        // 获取或创建会话历史
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = "session-" + System.currentTimeMillis();
        }

        List<Message> history = sessionHistory.computeIfAbsent(sessionId, k -> new ArrayList<>());

        // 调用 AI
        ChatResponse response = chatService.chat(request.getMessage(), history);

        // 保存历史
        history.add(Message.builder()
                .role("user")
                .content(request.getMessage())
                .build());
        history.add(Message.builder()
                .role("assistant")
                .content(response.getContent())
                .build());

        // 限制历史长度
        while (history.size() > 20) {
            history.remove(0);
        }

        response.setSessionId(sessionId);
        return R.ok(response);
    }

    /**
     * 清除会话历史
     */
    @DeleteMapping("/chat/{sessionId}")
    public R<Void> clearSession(@PathVariable String sessionId) {
        sessionHistory.remove(sessionId);
        return R.ok();
    }

    /**
     * 获取会话历史
     */
    @GetMapping("/chat/{sessionId}/history")
    public R<List<Message>> getHistory(@PathVariable String sessionId) {
        List<Message> history = sessionHistory.getOrDefault(sessionId, new ArrayList<>());
        return R.ok(history);
    }
}

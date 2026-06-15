package com.wo.agent.controller;

import com.wo.agent.entity.AiSession;
import com.wo.agent.memory.SessionManager;
import com.wo.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 会话管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionManager sessionManager;

    /**
     * 创建新会话
     */
    @PostMapping
    public Result<String> createSession(
            @RequestParam(required = false) Long workOrderId) {
        // TODO: 从 SecurityContext 获取当前用户ID
        Long userId = 1L;
        log.info("Creating session for user: {}, workOrderId: {}", userId, workOrderId);
        String sessionId = sessionManager.createSession(userId, workOrderId);
        return Result.success(sessionId);
    }

    /**
     * 查询用户的会话列表
     */
    @GetMapping
    public Result<List<AiSession>> listUserSessions() {
        // TODO: 从 SecurityContext 获取当前用户ID
        Long userId = 1L;
        log.info("Listing sessions for user: {}", userId);
        List<AiSession> sessions = sessionManager.getUserSessions(userId);
        return Result.success(sessions);
    }

    /**
     * 获取会话详情
     */
    @GetMapping("/{id}")
    public Result<AiSession> getSessionDetail(@PathVariable String id) {
        log.info("Getting session detail: {}", id);
        AiSession session = sessionManager.getSession(id);
        if (session == null) {
            return Result.fail("会话不存在");
        }
        return Result.success(session);
    }

    /**
     * 结束会话
     */
    @DeleteMapping("/{id}")
    public Result<Void> endSession(@PathVariable String id) {
        log.info("Ending session: {}", id);
        sessionManager.endSession(id);
        return Result.success();
    }
}

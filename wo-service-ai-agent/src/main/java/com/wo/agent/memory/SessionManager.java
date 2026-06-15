package com.wo.agent.memory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wo.agent.entity.AiSession;
import com.wo.agent.mapper.AiSessionMapper;
import com.wo.common.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * AI 会话管理服务
 * 管理会话的创建、更新和关闭
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionManager {

    private final AiSessionMapper sessionMapper;
    private final ConversationMemory conversationMemory;
    private final RedisService redisService;

    private static final String SESSION_ACTIVE_KEY = "ai:session:active:";

    @Value("${agent.memory.window-size:20}")
    private int windowSize;

    /**
     * 创建新会话
     *
     * @param userId      用户ID
     * @param workOrderId 关联工单ID（可选）
     * @return 会话ID
     */
    @Transactional(rollbackFor = Exception.class)
    public String createSession(Long userId, Long workOrderId) {
        // 生成 UUID 作为会话ID
        String sessionId = UUID.randomUUID().toString().replace("-", "");

        AiSession session = new AiSession();
        session.setId(sessionId);
        session.setUserId(userId);
        session.setWorkOrderId(workOrderId);
        session.setTitle(generateTitle(workOrderId));
        session.setStatus(1);
        session.setTotalMessages(0);
        session.setTotalTokens(0L);
        session.setLastActiveAt(LocalDateTime.now());

        sessionMapper.insert(session);

        // 在 Redis 中标记为活跃会话
        redisService.set(SESSION_ACTIVE_KEY + sessionId, "1", 3600 * 24); // 24小时过期

        log.info("Created new session: {} for user: {}, workOrder: {}", sessionId, userId, workOrderId);
        return sessionId;
    }

    /**
     * 更新会话活跃状态
     *
     * @param sessionId 会话ID
     */
    public void updateSessionActivity(String sessionId) {
        try {
            // 更新数据库
            AiSession session = sessionMapper.selectById(sessionId);
            if (session != null) {
                session.setLastActiveAt(LocalDateTime.now());
                session.setTotalMessages(session.getTotalMessages() + 1);
                sessionMapper.updateById(session);
            }

            // 续期 Redis key
            redisService.expire(SESSION_ACTIVE_KEY + sessionId, 3600 * 24);

        } catch (Exception e) {
            log.error("Failed to update session activity: {}", sessionId, e);
        }
    }

    /**
     * 结束会话
     *
     * @param sessionId 会话ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void endSession(String sessionId) {
        try {
            // 更新数据库状态
            AiSession session = sessionMapper.selectById(sessionId);
            if (session != null) {
                session.setStatus(0);
                sessionMapper.updateById(session);
            }

            // 清除 Redis 缓存
            redisService.delete(SESSION_ACTIVE_KEY + sessionId);
            conversationMemory.clearSession(sessionId);

            log.info("Ended session: {}", sessionId);

        } catch (Exception e) {
            log.error("Failed to end session: {}", sessionId, e);
        }
    }

    /**
     * 获取用户的会话列表
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    public List<AiSession> getUserSessions(Long userId) {
        return sessionMapper.selectList(
                new LambdaQueryWrapper<AiSession>()
                        .eq(AiSession::getUserId, userId)
                        .orderByDesc(AiSession::getLastActiveAt));
    }

    /**
     * 获取会话详情
     *
     * @param sessionId 会话ID
     * @return 会话信息
     */
    public AiSession getSession(String sessionId) {
        return sessionMapper.selectById(sessionId);
    }

    /**
     * 检查会话是否活跃
     *
     * @param sessionId 会话ID
     * @return 是否活跃
     */
    public boolean isSessionActive(String sessionId) {
        return redisService.hasKey(SESSION_ACTIVE_KEY + sessionId);
    }

    /**
     * 生成会话标题
     */
    private String generateTitle(Long workOrderId) {
        if (workOrderId != null) {
            return "工单 #" + workOrderId + " 咨询";
        }
        return "新对话 - " + LocalDateTime.now().toString().substring(0, 16);
    }
}

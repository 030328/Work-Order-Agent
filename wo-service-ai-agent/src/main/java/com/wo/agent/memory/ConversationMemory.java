package com.wo.agent.memory;

import com.wo.common.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 会话记忆服务
 * 基于 Redis 管理对话历史，支持滑动窗口和记忆压缩
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationMemory {

    private final RedisService redisService;

    private static final String SESSION_KEY_PREFIX = "ai:session:";
    private static final String MESSAGES_SUFFIX = ":messages";

    @Value("${agent.memory.window-size:20}")
    private int windowSize;

    @Value("${agent.memory.summarize-threshold:15}")
    private int summarizeThreshold;

    /**
     * 加载会话历史消息
     *
     * @param sessionId 会话ID
     * @return 消息列表（按时间正序）
     */
    public List<String> loadMessages(String sessionId) {
        String key = getSessionKey(sessionId);

        try {
            // 从 Redis List 获取消息（LRANGE 获取最近的消息）
            List<String> messages = redisService.lRange(key, 0, windowSize - 1);

            if (messages == null || messages.isEmpty()) {
                return new ArrayList<>();
            }

            // 反转为正序（LPUSH 存储的是倒序）
            Collections.reverse(messages);
            log.debug("Loaded {} messages for session: {}", messages.size(), sessionId);
            return messages;

        } catch (Exception e) {
            log.error("Failed to load messages for session: {}", sessionId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 保存消息到会话记忆
     *
     * @param sessionId 会话ID
     * @param role      角色（user/assistant/system/tool）
     * @param content   消息内容
     */
    public void saveMessage(String sessionId, String role, String content) {
        String key = getSessionKey(sessionId);

        try {
            // 格式化消息：role:content
            String formattedMessage = String.format("[%s] %s: %s",
                    java.time.LocalTime.now().toString().substring(0, 8),
                    role,
                    truncateContent(content, 1000));

            // LPUSH 到列表头部
            redisService.lPush(key, formattedMessage);

            // LTRIM 保持窗口大小（保留最近的 N 条消息）
            redisService.lTrim(key, 0, windowSize * 2 - 1);

            log.debug("Saved {} message to session: {}, content length: {}",
                    role, sessionId, content.length());

        } catch (Exception e) {
            log.error("Failed to save message to session: {}", sessionId, e);
        }
    }

    /**
     * 清空会话记忆
     *
     * @param sessionId 会话ID
     */
    public void clearSession(String sessionId) {
        String key = getSessionKey(sessionId);

        try {
            redisService.delete(key);
            log.info("Cleared memory for session: {}", sessionId);
        } catch (Exception e) {
            log.error("Failed to clear session: {}", sessionId, e);
        }
    }

    /**
     * 获取会话消息数量
     *
     * @param sessionId 会话ID
     * @return 消息数量
     */
    public long getMessageCount(String sessionId) {
        String key = getSessionKey(sessionId);
        try {
            return redisService.lLen(key);
        } catch (Exception e) {
            log.error("Failed to get message count for session: {}", sessionId, e);
            return 0;
        }
    }

    /**
     * 检查是否需要进行记忆压缩
     *
     * @param sessionId 会话ID
     * @return 是否需要压缩
     */
    public boolean needsSummarization(String sessionId) {
        return getMessageCount(sessionId) > summarizeThreshold;
    }

    /**
     * 获取会话 Redis Key
     */
    private String getSessionKey(String sessionId) {
        return SESSION_KEY_PREFIX + sessionId + MESSAGES_SUFFIX;
    }

    /**
     * 截断过长的内容
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...(truncated)";
    }
}

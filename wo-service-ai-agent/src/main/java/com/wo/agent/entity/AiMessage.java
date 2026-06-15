package com.wo.agent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wo.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI消息实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_message")
public class AiMessage extends BaseEntity {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 角色：user-用户, assistant-助手, system-系统, tool-工具
     */
    private String role;

    /**
     * 消息内容（长文本）
     */
    private String content;

    /**
     * 工具调用信息（JSON字符串，长文本）
     */
    private String toolCalls;

    /**
     * 工具调用ID
     */
    private String toolCallId;

    /**
     * Token消耗
     */
    private Integer tokenCount;
}

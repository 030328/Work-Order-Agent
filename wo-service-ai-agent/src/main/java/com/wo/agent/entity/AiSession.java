package com.wo.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wo.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * AI会话实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_session")
public class AiSession extends BaseEntity {

    /**
     * 会话ID（UUID）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 会话标题
     */
    private String title;

    /**
     * 关联工单ID
     */
    private Long workOrderId;

    /**
     * 状态：1-活跃, 0-已结束
     */
    private Integer status = 1;

    /**
     * 总消息数
     */
    private Integer totalMessages = 0;

    /**
     * 总Token消耗
     */
    private Long totalTokens = 0L;

    /**
     * 最后活跃时间
     */
    private LocalDateTime lastActiveAt;
}

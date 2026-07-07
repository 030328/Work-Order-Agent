package com.wo.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI会话实体
 */
@TableName("ai_session")
@Data
public class AiSession implements Serializable {

    /**
     * 会话ID
     */
    @TableId(type = IdType.INPUT)
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
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 最后活跃时间
     */
    private LocalDateTime lastActiveAt;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    private Integer deleted;
}

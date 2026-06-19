package com.wo.workorder.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wo.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wo_work_order")
public class WoWorkOrder extends BaseEntity {

    private String orderNo;

    private String title;

    /**
     * 工单描述
     */
    private String description;

    /**
     * 工单分类
     */
    private String category;

    /**
     * 优先级: LOW, MEDIUM, HIGH, URGENT
     */
    private String priority;

    /**
     * 状态: DRAFT, OPEN, IN_PROGRESS, RESOLVED, CLOSED
     */
    private String status;

    /**
     * 创建人ID
     */
    private Long creatorId;

    /**
     * 处理人ID
     */
    private Long assigneeId;

    /**
     * 所属部门
     */
    private String department;

    /**
     * SLA截止时间
     */
    private LocalDateTime slaDeadline;

    /**
     * 解决时间
     */
    private LocalDateTime resolvedAt;

    /**
     * 关闭时间
     */
    private LocalDateTime closedAt;

    /**
     * 解决方案
     */
    private String resolution;

    /**
     * 标签，逗号分隔
     */
    private String tags;

    /**
     * AI生成的摘要
     */
    private String aiSummary;

    /**
     * AI情感分析结果
     */
    private String aiSentiment;

    /**
     * AI分类建议
     */
    private String aiCategorySuggestion;

    /**
     * AI建议解决方案
     */
    private String aiSuggestedSolution;

    /**
     * 转人工时间
     */
    private LocalDateTime escalatedAt;

    /**
     * 认领时间
     */
    private LocalDateTime claimedAt;
}

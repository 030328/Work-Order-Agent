package com.wo.agent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wo.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库文档实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_document")
public class KbDocument extends BaseEntity {

    /**
     * 文档标题
     */
    private String title;

    /**
     * 文档内容（长文本）
     */
    private String content;

    /**
     * 来源类型：manual-手动录入, workorder-工单, faq-FAQ, document-文档
     */
    private String sourceType;

    /**
     * 来源ID（如工单ID）
     */
    private String sourceId;

    /**
     * 分类
     */
    private String category;

    /**
     * 分块数量
     */
    private Integer chunkCount = 0;

    /**
     * 状态：1-启用, 0-禁用
     */
    private Integer status = 1;

    /**
     * 是否人工验证：0-AI自动生成, 1-人工确认正确
     */
    private Integer verified = 0;

    /**
     * 点赞数
     */
    private Integer likeCount = 0;

    /**
     * 创建人ID
     */
    private Long createdBy;
}

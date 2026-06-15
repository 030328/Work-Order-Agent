package com.wo.workorder.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wo.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wo_comment")
public class WoComment extends BaseEntity {

    /**
     * 工单ID
     */
    private Long workOrderId;

    /**
     * 评论用户ID
     */
    private Long userId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 是否内部评论: 0-否, 1-是
     */
    private Integer isInternal;

    /**
     * 是否AI生成: 0-否, 1-是
     */
    private Integer isAiGenerated;
}

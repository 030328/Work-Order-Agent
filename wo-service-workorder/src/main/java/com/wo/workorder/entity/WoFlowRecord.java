package com.wo.workorder.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wo.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wo_flow_record")
public class WoFlowRecord extends BaseEntity {

    /**
     * 工单ID
     */
    private Long workOrderId;

    /**
     * 操作动作: CREATE, STATUS_CHANGE, ASSIGN, COMMENT, CLOSE
     */
    private String action;

    /**
     * 原状态
     */
    private String fromStatus;

    /**
     * 目标状态
     */
    private String toStatus;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作备注
     */
    private String comment;

    /**
     * 附件URL，逗号分隔
     */
    private String attachmentUrls;

    /**
     * 是否系统操作: 0-否, 1-是
     */
    private Integer isSystem;
}

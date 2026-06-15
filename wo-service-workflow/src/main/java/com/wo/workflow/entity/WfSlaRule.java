package com.wo.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wo.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_sla_rule")
public class WfSlaRule extends BaseEntity {

    /**
     * SLA rule name
     */
    private String name;

    /**
     * Priority level (e.g., P1, P2, P3, P4)
     */
    private String priority;

    /**
     * Response time in hours
     */
    private Integer responseHours;

    /**
     * Resolution time in hours
     */
    private Integer resolveHours;

    /**
     * Escalation assignee user ID
     */
    private Long escalationAssigneeId;

    /**
     * Active flag: 1-active, 0-inactive
     */
    private Integer isActive = 1;
}

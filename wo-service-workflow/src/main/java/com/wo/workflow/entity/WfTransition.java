package com.wo.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wo.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_transition")
public class WfTransition extends BaseEntity {

    /**
     * Workflow definition ID
     */
    private Long definitionId;

    /**
     * Source state
     */
    private String fromState;

    /**
     * Target state
     */
    private String toState;

    /**
     * Trigger event
     */
    private String event;

    /**
     * SpEL guard condition
     */
    private String guardCondition;

    /**
     * Action class bean name to execute on transition
     */
    private String actionClass;

    /**
     * Required role for this transition
     */
    private String requiredRole;

    /**
     * Sort order
     */
    private Integer sortOrder = 0;
}

package com.wo.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wo.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_definition")
public class WfDefinition extends BaseEntity {

    /**
     * Workflow name
     */
    private String name;

    /**
     * Workflow description
     */
    private String description;

    /**
     * State machine definition JSON
     */
    private String definitionJson;

    /**
     * Version number
     */
    private Integer version = 1;

    /**
     * Status: 1-active, 0-inactive
     */
    private Integer status = 1;
}

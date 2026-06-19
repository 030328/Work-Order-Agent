package com.wo.api.dto.workflow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class TransitionRequest implements Serializable {

    @NotNull(message = "工单ID不能为空")
    private Long workOrderId;

    private Long definitionId;

    @NotBlank(message = "当前状态不能为空")
    private String fromStatus;

    @NotBlank(message = "目标状态不能为空")
    private String toStatus;

    private String event;

    @NotNull(message = "操作人ID不能为空")
    private Long operatorId;

    private String operatorRole;

    private String comment;
}

package com.wo.api.dto.workflow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class TransitionRequest implements Serializable {

    @NotNull(message = "工单ID不能为空")
    private Long workOrderId;

    @NotBlank(message = "目标状态不能为空")
    private String targetStatus;

    @NotNull(message = "操作人ID不能为空")
    private Long operatorId;

    private String comment;
}

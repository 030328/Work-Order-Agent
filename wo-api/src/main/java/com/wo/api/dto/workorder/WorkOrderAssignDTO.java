package com.wo.api.dto.workorder;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class WorkOrderAssignDTO implements Serializable {

    @NotNull(message = "assigneeId is required")
    @Positive(message = "assigneeId must be positive")
    private Long assigneeId;

    @Size(max = 1000, message = "reason must not exceed 1000 characters")
    private String reason;
}

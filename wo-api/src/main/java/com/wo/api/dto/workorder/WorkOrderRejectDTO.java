package com.wo.api.dto.workorder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class WorkOrderRejectDTO implements Serializable {

    @NotBlank(message = "reason is required")
    @Size(max = 1000, message = "reason must not exceed 1000 characters")
    private String reason;
}
